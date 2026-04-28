import { openDB } from "idb";
import type { Instrument, PortfolioResponse, WsQuoteEvent } from "../types/api";

interface CacheDb {
  portfolio: {
    key: string;
    value: PortfolioResponse;
  };
  instruments: {
    key: string;
    value: Instrument[];
  };
  quotes: {
    key: string;
    value: WsQuoteEvent;
  };
}

const dbPromise = openDB<CacheDb>("trading-terminal-cache", 1, {
  upgrade(db) {
    db.createObjectStore("portfolio");
    db.createObjectStore("instruments");
    db.createObjectStore("quotes");
  }
});

export const offlineCache = {
  async setPortfolio(data: PortfolioResponse): Promise<void> {
    const db = await dbPromise;
    await db.put("portfolio", data, "latest");
  },
  async getPortfolio(): Promise<PortfolioResponse | null> {
    const db = await dbPromise;
    return (await db.get("portfolio", "latest")) ?? null;
  },
  async setInstruments(query: string, items: Instrument[]): Promise<void> {
    const db = await dbPromise;
    await db.put("instruments", items, query);
  },
  async getInstruments(query: string): Promise<Instrument[] | null> {
    const db = await dbPromise;
    return (await db.get("instruments", query)) ?? null;
  },
  async setQuote(quote: WsQuoteEvent): Promise<void> {
    const db = await dbPromise;
    await db.put("quotes", quote, quote.ticker);
  },
  async getQuote(ticker: string): Promise<WsQuoteEvent | null> {
    const db = await dbPromise;
    return (await db.get("quotes", ticker)) ?? null;
  }
};
