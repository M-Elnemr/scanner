"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";

const STORAGE_KEY = "us_compare_ids";
const MAX_COMPARE = 4;

interface CompareContextValue {
  ids: string[];
  isSelected: (id: string) => boolean;
  toggle: (id: string) => void;
  clear: () => void;
  max: number;
}

const CompareContext = createContext<CompareContextValue | null>(null);

export function CompareProvider({ children }: { children: React.ReactNode }) {
  const [ids, setIds] = useState<string[]>([]);

  useEffect(() => {
    // Reading localStorage in a lazy useState initializer would run during SSR/hydration too,
    // where the value differs from the server-rendered [], causing a hydration mismatch — this
    // has to run post-mount instead.
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (stored) setIds(JSON.parse(stored));
    } catch {
      // ignore corrupted storage
    }
  }, []);

  const persist = useCallback((next: string[]) => {
    setIds(next);
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }, []);

  const toggle = useCallback(
    (id: string) => {
      setIds((current) => {
        const next = current.includes(id)
          ? current.filter((x) => x !== id)
          : current.length >= MAX_COMPARE
            ? current
            : [...current, id];
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        return next;
      });
    },
    [],
  );

  const clear = useCallback(() => persist([]), [persist]);

  return (
    <CompareContext.Provider
      value={{ ids, isSelected: (id) => ids.includes(id), toggle, clear, max: MAX_COMPARE }}
    >
      {children}
    </CompareContext.Provider>
  );
}

export function useCompare() {
  const ctx = useContext(CompareContext);
  if (!ctx) throw new Error("useCompare must be used within CompareProvider");
  return ctx;
}
