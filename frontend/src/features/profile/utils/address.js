import provinces from '../data/vietnam-addresses.json' with { type: 'json' }

export { provinces }

export function searchText(value) {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').toLowerCase().replace(/\s+/g, ' ').trim()
}

function provinceName(value) {
  return searchText(value).replace(/^(tinh|thanh pho|tp\.?)[\s]+/, '')
}

export function readAddress({ city, address }) {
  const province = provinces.find(item => provinceName(item.name) === provinceName(city || ''))
  const originalAddress = address || ''
  const parts = originalAddress.split(',')
  const lastPart = parts.at(-1).trim()
  const matches = province?.wards.filter(ward => searchText(ward.name) === searchText(lastPart)) || []
  const ward = matches.length === 1 ? matches[0] : null

  return {
    provinceCode: province?.code || '',
    legacyCity: province ? '' : city || '',
    wardCode: ward?.code || '',
    detail: ward ? parts.slice(0, -1).join(',').trim() : originalAddress,
  }
}

export function writeAddress(location) {
  const province = provinces.find(item => item.code === location.provinceCode)
  const ward = province?.wards.find(item => item.code === location.wardCode)
  return {
    city: province?.name || location.legacyCity || '',
    address: [location.detail.trim(), ward?.name].filter(Boolean).join(', '),
  }
}

export function addressPayload(original, location) {
  const unchanged = JSON.stringify(location) === JSON.stringify(readAddress(original))
  return unchanged ? { city: original.city, address: original.address } : writeAddress(location)
}
