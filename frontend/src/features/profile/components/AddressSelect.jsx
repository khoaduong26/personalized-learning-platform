import { useEffect, useRef, useState } from 'react'
import { searchText } from '../utils/address'

export default function AddressSelect({ id, label, options, value, onChange, disabled = false, placeholder }) {
  const list = useRef(null)
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(0)
  const selected = options.find(option => option.code === value)
  const filtered = options.filter(option => searchText(option.name).includes(searchText(query)))
  const choices = query.trim() ? filtered : [{ code: '', name: 'Chưa chọn' }, ...filtered]
  const expanded = open && !disabled
  const activeIndex = Math.min(active, choices.length - 1)

  useEffect(() => {
    if (expanded) list.current?.children[activeIndex]?.scrollIntoView({ block: 'nearest' })
  }, [expanded, activeIndex, query])

  function showOptions() {
    setQuery('')
    setActive(0)
    setOpen(true)
  }

  function choose(option) {
    onChange(option.code)
    setOpen(false)
    setQuery('')
  }

  function keyDown(event) {
    if (event.key === 'Escape') {
      event.preventDefault()
      setOpen(false)
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      if (!expanded) showOptions()
      else setActive(index => Math.max(0, Math.min(choices.length - 1, index + (event.key === 'ArrowDown' ? 1 : -1))))
    } else if (event.key === 'Enter' && expanded) {
      event.preventDefault()
      if (choices[activeIndex]) choose(choices[activeIndex])
    }
  }

  return <div className="profile-field">
    <label htmlFor={id}>{label}</label>
    <div className="profile-address-select" onBlur={event => {
      if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
    }}>
      <input id={id} role="combobox" autoComplete="off" disabled={disabled}
        value={expanded ? query : selected?.name || ''} placeholder={selected?.name || placeholder}
        aria-expanded={expanded} aria-controls={`${id}-options`} aria-autocomplete="list"
        aria-activedescendant={expanded && activeIndex >= 0 ? `${id}-option-${activeIndex}` : undefined}
        onFocus={showOptions} onClick={() => { if (!expanded) showOptions() }}
        onChange={event => { setQuery(event.target.value); setActive(0); setOpen(true) }} onKeyDown={keyDown} />
      <span className="profile-select-chevron" aria-hidden="true">⌄</span>
      {expanded && <div className="profile-address-popup">
        <ul ref={list} id={`${id}-options`} role="listbox" aria-label={label}>
          {choices.map((option, index) => <li key={option.code} role="presentation">
            <button id={`${id}-option-${index}`} role="option" type="button" tabIndex={-1}
              aria-selected={option.code === value} className={index === activeIndex ? 'is-active' : ''}
              onMouseDown={event => event.preventDefault()} onMouseEnter={() => setActive(index)} onClick={() => choose(option)}>
              {option.name}{option.code === value && <span aria-hidden="true">✓</span>}
            </button>
          </li>)}
        </ul>
        {filtered.length === 0 && <p role="status">Không tìm thấy địa chỉ phù hợp.</p>}
      </div>}
    </div>
  </div>
}
