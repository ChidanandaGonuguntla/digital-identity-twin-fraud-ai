export default function Panel({title, kicker, right, children, className = '', bodyClass = ''}) {
    return (
        <section className={`panel ${className}`}>
            {(title || kicker || right) && (
                <header className="flex items-start justify-between gap-3 border-b border-line/70 px-5 py-3.5">
                    <div>
                        {kicker && <div className="label-kicker mb-1">{kicker}</div>}
                        {title && <h3 className="font-semibold tracking-tight text-ink">{title}</h3>}
                    </div>
                    {right}
                </header>
            )}
            <div className={`${bodyClass || 'p-5'}`}>{children}</div>
        </section>
    );
}
