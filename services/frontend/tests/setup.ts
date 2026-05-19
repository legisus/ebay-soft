import "@testing-library/jest-dom";
import { afterEach, vi } from "vitest";

// Each test starts with a clean localStorage and no cached fetch state.
afterEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});
