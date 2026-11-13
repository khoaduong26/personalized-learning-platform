export default function LearningResourceCard({ resource, className = '' }) {
  return (
    <div
      className={`relative flex aspect-3/4 flex-col justify-between overflow-hidden rounded-[3px] p-4 shadow-[0_1px_0_rgba(0,0,0,0.08),0_12px_24px_-12px_rgba(40,30,10,0.45)] ${className}`}
      style={{ backgroundColor: resource.bg, color: resource.fg }}
    >
      {/* gáy sách */}
      <span className="absolute inset-y-0 left-0 w-2 bg-black/15" />
      <div className="pl-2 text-[10px] font-medium uppercase tracking-[0.18em] opacity-80">
        {resource.series}
      </div>
      <div className="pl-2">
        <div className="font-serif text-xl font-semibold leading-tight">{resource.title}</div>
        <div className="mt-3 h-px w-8" style={{ backgroundColor: resource.fg, opacity: 0.6 }} />
        <div className="mt-2 text-[11px] uppercase tracking-widest opacity-80">
          Lớp {resource.grade}
        </div>
      </div>
    </div>
  )
}
