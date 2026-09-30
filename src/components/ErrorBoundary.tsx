import { Component, type ReactNode } from 'react';

/** Pojistka: místo prázdné obrazovky ukáže zprávu a nabídne novou úlohu. */
export class ErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  render() {
    if (!this.state.error) return this.props.children;
    const ru = document.documentElement.lang === 'ru';
    return (
      <div className="app">
        <section className="panel crash">
          <h2>{ru ? 'Что-то пошло не так' : 'Něco se pokazilo'}</h2>
          <p className="muted small">{this.state.error.message}</p>
          <button
            className="btn btn-primary"
            onClick={() => {
              location.hash = '';
              location.reload();
            }}
          >
            {ru ? 'Новая задача' : 'Nová úloha'}
          </button>
        </section>
      </div>
    );
  }
}
