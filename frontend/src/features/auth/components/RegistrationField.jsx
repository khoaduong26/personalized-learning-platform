export default function RegistrationField({ label, name, error, ...props }) {
  return (
    <label className="registration-field" htmlFor={name}>
      <span>{label}</span>
      <input id={name} name={name} required aria-invalid={!!error} aria-describedby={error ? `${name}-error` : undefined} {...props} />
      {error && <small id={`${name}-error`} className="registration-field-error">{error}</small>}
    </label>
  )
}
