import { useEffect, useState } from 'react'
import { AlertCircle, ArrowUpRight, Landmark, RefreshCw } from 'lucide-react'

const API_URL = 'http://localhost:8080/api/propositions'

export default function PropositionFeed() {
  const [propositions, setPropositions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [requestVersion, setRequestVersion] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function fetchPropositions() {
      setLoading(true)
      setError('')
      try {
        const response = await fetch(API_URL, {
          signal: controller.signal,
          headers: { Accept: 'application/json' },
        })
        if (!response.ok) {
          throw new Error(`O servidor respondeu com erro ${response.status}.`)
        }
        const data = await response.json()
        if (!Array.isArray(data) || data.some((item) => !item || item.id == null)) {
          throw new Error('O servidor retornou dados em um formato inesperado.')
        }
        if (!controller.signal.aborted) setPropositions(data)
      } catch (failure) {
        if (!controller.signal.aborted) {
          setError(failure instanceof TypeError
            ? 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.'
            : failure.message || 'Não foi possível carregar as propostas.')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    fetchPropositions()
    return () => controller.abort()
  }, [requestVersion])

  function renderTranslatedSummary(summary) {
    if (typeof summary !== 'string' || !summary.trim()) {
      return <p className="text-sm text-zinc-500">Tradução ainda não disponível.</p>
    }
    const headingStyles = {
      '**A Situação Atual**': 'text-zinc-700',
      '**O Que Muda**': 'text-emerald-800',
      '**O Impacto no Seu Bolso**': 'text-rose-800',
    }
    return summary.split(/(\*\*A Situação Atual\*\*|\*\*O Que Muda\*\*|\*\*O Impacto no Seu Bolso\*\*)/g)
      .map((part, index) => {
        if (Object.hasOwn(headingStyles, part)) {
          return (
            <h3 key={index} className={`mb-2 mt-6 text-sm font-bold first:mt-0 ${headingStyles[part]}`}>
              {part.slice(2, -2)}
            </h3>
          )
        }
        const text = part.trim().replace(/^:\s*/, '')
        return text ? (
          <p key={index} className="whitespace-pre-line text-sm leading-7 text-zinc-600 sm:text-base">
            {text}
          </p>
        ) : null
      })
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-zinc-200 bg-white/90">
        <div className="mx-auto flex max-w-3xl items-center gap-3 px-5 py-5 sm:px-8">
          <Landmark className="size-6 shrink-0 text-emerald-800" aria-hidden="true" />
          <span className="font-editorial text-2xl font-semibold">Política em claro</span>
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-5 py-8 sm:px-8 sm:py-12">
        <div className="mb-7 flex items-end justify-between gap-4">
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-normal text-emerald-800">Câmara dos Deputados</p>
            <h1 className="font-editorial text-3xl font-semibold sm:text-4xl">Propostas em pauta</h1>
          </div>
          <button type="button" title="Atualizar propostas" aria-label="Atualizar propostas" disabled={loading}
            onClick={() => setRequestVersion((version) => version + 1)}
            className="flex size-11 shrink-0 items-center justify-center rounded-md border border-zinc-200 bg-white text-zinc-600 transition-colors hover:bg-zinc-100 disabled:cursor-wait disabled:opacity-50">
            <RefreshCw className={`size-4 ${loading ? 'motion-safe:animate-spin' : ''}`} aria-hidden="true" />
          </button>
        </div>

        {loading ? (
          <div role="status" aria-label="Carregando propostas" className="space-y-5">
            <span className="sr-only">Carregando propostas...</span>
            {[0, 1, 2].map((item) => (
              <div key={item} aria-hidden="true" className="rounded-lg border border-zinc-200 bg-white p-6 shadow-sm motion-safe:animate-pulse">
                <div className="mb-7 h-5 w-28 rounded bg-zinc-100" />
                <div className="mb-3 h-3 w-2/3 rounded bg-zinc-100" />
                <div className="mb-3 h-3 rounded bg-zinc-100" />
                <div className="h-3 w-4/5 rounded bg-zinc-100" />
              </div>
            ))}
          </div>
        ) : error ? (
          <div role="alert" className="border-l-2 border-rose-700 bg-rose-50 p-5">
            <div className="flex items-center gap-2 font-semibold text-rose-900">
              <AlertCircle className="size-5 shrink-0" aria-hidden="true" />Não foi possível carregar
            </div>
            <p className="mt-2 text-sm leading-6 text-rose-900">{error}</p>
            <button type="button" onClick={() => setRequestVersion((version) => version + 1)} className="mt-4 inline-flex min-h-11 items-center gap-2 text-sm font-semibold text-rose-900 underline underline-offset-4">
              <RefreshCw className="size-4" aria-hidden="true" /> Tentar novamente
            </button>
          </div>
        ) : propositions.length === 0 ? (
          <p role="status" className="border-t border-zinc-200 py-10 text-center text-zinc-500">Nenhuma proposta disponível no momento.</p>
        ) : (
          <div className="space-y-5">
            {propositions.map((proposition) => (
              <article key={proposition.id} aria-labelledby={`proposition-${proposition.id}`} className="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-sm">
                <div className="px-5 py-6 sm:px-8 sm:py-7">
                  <div className="mb-6 flex flex-wrap items-center gap-3">
                    <span className={`rounded px-2 py-1 text-xs font-bold ${proposition.type === 'PEC' ? 'bg-rose-50 text-rose-800' : 'bg-emerald-50 text-emerald-800'}`}>{proposition.type}</span>
                    <h2 id={`proposition-${proposition.id}`} className="text-base font-bold text-zinc-800">{proposition.type} {proposition.number}/{proposition.year}</h2>
                  </div>
                  <div className="break-words [overflow-wrap:anywhere]">{renderTranslatedSummary(proposition.translatedSummary)}</div>
                  <details className="mt-6 border-t border-zinc-100 pt-4">
                    <summary className="min-h-8 cursor-pointer text-sm font-medium text-zinc-500 hover:text-zinc-800">Texto original</summary>
                    <p className="mt-3 whitespace-pre-line break-words text-sm leading-6 text-zinc-500 [overflow-wrap:anywhere]">{proposition.originalSummary || 'Texto original indisponível.'}</p>
                  </details>
                </div>
                <footer className="flex items-center justify-between gap-3 border-t border-zinc-100 bg-zinc-50/60 px-5 py-3 text-xs text-zinc-500 sm:px-8">
                  <span>ID {proposition.id}</span>
                  <a href={`https://www.camara.leg.br/propostas-legislativas/${encodeURIComponent(proposition.id)}`} target="_blank" rel="noopener noreferrer" className="inline-flex min-h-8 items-center gap-1 font-medium text-emerald-800 hover:underline">
                    Fonte oficial <ArrowUpRight className="size-3.5" aria-hidden="true" />
                  </a>
                </footer>
              </article>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}