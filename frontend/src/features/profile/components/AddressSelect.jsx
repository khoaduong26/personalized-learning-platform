import { useEffect, useRef, useState } from "react";
import { searchText } from "../utils/address";
import ProfileIcon from "./ProfileIcon";

export default function AddressSelect({
  id,
  label,
  options,
  value,
  onChange,
  disabled = false,
  placeholder,
}) {
  const container = useRef(null);
  const trigger = useRef(null);
  const search = useRef(null);
  const list = useRef(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const [placement, setPlacement] = useState("below");
  const selected = options.find((option) => option.code === value);
  const filtered = options.filter((option) =>
    searchText(option.name).includes(searchText(query)),
  );
  const expanded = open && !disabled;
  const activeIndex = Math.min(active, filtered.length - 1);

  useEffect(() => {
    if (expanded) search.current?.focus();
  }, [expanded]);

  useEffect(() => {
    if (expanded)
      list.current?.children[activeIndex]?.scrollIntoView({ block: "nearest" });
  }, [expanded, activeIndex, query]);

  useEffect(() => {
    if (!expanded) return;
    function outside(event) {
      if (!container.current?.contains(event.target)) setOpen(false);
    }
    document.addEventListener("pointerdown", outside);
    return () => document.removeEventListener("pointerdown", outside);
  }, [expanded]);

  function showOptions() {
    const bounds = trigger.current.getBoundingClientRect();
    setPlacement(
      window.innerHeight - bounds.bottom < 340 && bounds.top > 340
        ? "above"
        : "below",
    );
    setQuery("");
    setActive(
      Math.max(
        0,
        options.findIndex((option) => option.code === value),
      ),
    );
    setOpen(true);
  }

  function close() {
    setOpen(false);
    trigger.current?.focus();
  }

  function choose(code) {
    onChange(code);
    close();
  }

  function keyDown(event) {
    if (event.key === "Escape") {
      event.preventDefault();
      event.stopPropagation();
      close();
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      if (!expanded) showOptions();
      else
        setActive((index) =>
          Math.max(
            0,
            Math.min(
              filtered.length - 1,
              index + (event.key === "ArrowDown" ? 1 : -1),
            ),
          ),
        );
    } else if (event.key === "Enter" && event.target === search.current) {
      event.preventDefault();
      if (filtered[activeIndex]) choose(filtered[activeIndex].code);
    }
  }

  return (
    <div className="profile-field">
      <label id={`${id}-label`} htmlFor={id}>
        {label}
      </label>
      <div
        ref={container}
        className={`profile-address-select${expanded ? " is-open" : ""}`}
        onKeyDown={keyDown}
        onBlur={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget))
            setOpen(false);
        }}
      >
        <button
          ref={trigger}
          id={id}
          className={`profile-select-trigger${selected ? " has-value" : ""}`}
          type="button"
          role="combobox"
          disabled={disabled}
          aria-haspopup="dialog"
          aria-expanded={expanded}
          aria-controls={`${id}-popup`}
          aria-labelledby={`${id}-label ${id}-value`}
          aria-describedby={disabled ? `${id}-hint` : undefined}
          onClick={() => (expanded ? close() : showOptions())}
        >
          <span className="profile-select-symbol">
            <ProfileIcon name="pin" />
          </span>
          <span id={`${id}-value`} className="profile-select-value">
            {selected?.name || placeholder}
          </span>
          <span className="profile-select-chevron">
            <ProfileIcon name="chevron" />
          </span>
        </button>
        {expanded && (
          <div
            id={`${id}-popup`}
            role="dialog"
            aria-label={`Chọn ${label.toLowerCase()}`}
            className={`profile-address-popup is-${placement}`}
          >
            <div className="profile-address-search">
              <ProfileIcon name="search" />
              <input
                ref={search}
                role="combobox"
                aria-label={`Tìm ${label.toLowerCase()}`}
                autoComplete="off"
                placeholder="Tìm kiếm, có thể gõ không dấu…"
                value={query}
                aria-expanded="true"
                aria-autocomplete="list"
                aria-controls={`${id}-options`}
                aria-activedescendant={
                  activeIndex >= 0 ? `${id}-option-${activeIndex}` : undefined
                }
                onChange={(event) => {
                  setQuery(event.target.value);
                  setActive(0);
                }}
              />
              {query && (
                <button
                  type="button"
                  className="profile-search-clear"
                  aria-label="Xóa từ khóa"
                  onClick={() => {
                    setQuery("");
                    setActive(0);
                    search.current?.focus();
                  }}
                >
                  ×
                </button>
              )}
            </div>
            <ul
              ref={list}
              id={`${id}-options`}
              role="listbox"
              aria-label={label}
            >
              {filtered.map((option, index) => (
                <li key={option.code} role="presentation">
                  <button
                    id={`${id}-option-${index}`}
                    role="option"
                    type="button"
                    tabIndex={-1}
                    aria-selected={option.code === value}
                    className={index === activeIndex ? "is-active" : ""}
                    onMouseDown={(event) => event.preventDefault()}
                    onMouseEnter={() => setActive(index)}
                    onClick={() => choose(option.code)}
                  >
                    <span>{option.name}</span>
                    <span className="profile-option-check">
                      {option.code === value && <ProfileIcon name="check" />}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            {filtered.length === 0 && (
              <div className="profile-address-empty" role="status">
                <ProfileIcon name="search" />
                <p>Không tìm thấy địa chỉ phù hợp.</p>
                <small>Thử tên khác hoặc rút ngắn từ khóa.</small>
              </div>
            )}
            <div className="profile-address-footer">
              <span aria-live="polite">{filtered.length} địa chỉ</span>
              <button
                type="button"
                disabled={!value}
                onClick={() => choose("")}
              >
                Bỏ chọn
              </button>
            </div>
          </div>
        )}
      </div>
      {disabled && (
        <small id={`${id}-hint`} className="profile-hint">
          Chọn tỉnh / thành phố để xem danh sách.
        </small>
      )}
    </div>
  );
}
