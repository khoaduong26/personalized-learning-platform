import assert from 'node:assert/strict'
import test from 'node:test'
import { addressPayload, provinces, readAddress, searchText, writeAddress } from '../src/features/profile/utils/address.js'
import { profilePayload } from '../src/features/profile/utils/profile.js'

test('offline address data has unique province/ward codes and every ward belongs to a province', () => {
  assert.equal(provinces.length, 34)
  assert.equal(new Set(provinces.map(province => province.code)).size, 34)
  const wards = provinces.flatMap(province => province.wards)
  assert.equal(wards.length, 3321)
  assert.equal(new Set(wards.map(ward => ward.code)).size, wards.length)
  for (const province of provinces) {
    assert.match(province.code, /^\d{2}$/)
    assert.ok(province.name && province.wards.length)
    for (const ward of province.wards) assert.match(ward.code, /^\d{5}$/)
  }
})

test('search matches Vietnamese names with or without diacritics', () => {
  assert.equal(searchText('  Phường Thủ Đức '), 'phuong thu duc')
  assert.ok(searchText('Thành phố Hồ Chí Minh').includes(searchText('ho chi minh')))
})

test('new selections round trip through the existing city/address attributes', () => {
  const province = provinces.find(item => item.code === '79')
  const location = { provinceCode: province.code, legacyCity: '', wardCode: province.wards[0].code, detail: '12 Nguyễn Huệ, tầng 2' }
  const saved = writeAddress(location)
  assert.deepEqual(saved, { city: province.name, address: `12 Nguyễn Huệ, tầng 2, ${province.wards[0].name}` })
  assert.deepEqual(readAddress(saved), location)
  const form = { fullName: 'Tên mẫu', phone: '', ...saved, sex: '', dateOfBirth: '' }
  assert.deepEqual(Object.keys(profilePayload(form, 'TEACHER')).sort(), ['address', 'city', 'fullName', 'phone'])
})

test('editing other profile fields preserves legacy addresses and province spellings verbatim', () => {
  for (const original of [
    { city: 'Hồ Chí Minh', address: ' 12 Nguyễn Huệ, Phường Bến Nghé, Quận 1 ' },
    { city: 'Tỉnh cũ chưa nhận diện', address: 'Thôn cũ, Xã cũ, Huyện cũ' },
    { city: '', address: 'Địa chỉ đã nhập tự do' },
  ]) {
    assert.deepEqual(addressPayload(original, readAddress(original)), original)
  }
})

test('a ward name inside the street address is not removed unless it is the full final component', () => {
  const province = provinces[0]
  const original = { city: province.name, address: `Gần ${province.wards[0].name}, số 12` }
  assert.equal(readAddress(original).detail, original.address)
  assert.equal(readAddress(original).wardCode, '')
})

test('clearing a ward or changing province cannot save a ward from another province', () => {
  const location = { provinceCode: provinces[0].code, legacyCity: '', wardCode: provinces[1].wards[0].code, detail: 'Số 12' }
  assert.equal(writeAddress(location).address, 'Số 12')
  assert.equal(writeAddress({ ...location, wardCode: '' }).address, 'Số 12')
  assert.deepEqual(writeAddress({ ...location, provinceCode: '', wardCode: '' }), { city: '', address: 'Số 12' })
})

test('ward-only addresses reopen without duplicating the ward in the details', () => {
  const province = provinces[0]
  const original = { city: province.name, address: province.wards[0].name }
  const location = readAddress(original)
  assert.equal(location.detail, '')
  assert.deepEqual(writeAddress(location), original)
})
