---
trigger: model_decision
description: Automatically activated when the agent writes or modifies React components, TypeScript files, hooks, or any frontend code in the hotel booking system (React 19 + TypeScript + Vite + Tailwind CSS v4).
---

# Frontend Coding Standards — Hotel Booking System (React 19 + TypeScript + Tailwind CSS v4)

## 1. TypeScript Rules

- **Strict mode must be enabled** in `tsconfig.json` (`"strict": true`)
- Never use `any` — use proper types, `unknown`, or generics instead
- Define explicit return types on all functions and hooks that return non-trivial values
- Use `interface` for object shapes that may be extended; use `type` for union/intersection types and primitives
- All API response shapes must be typed using interfaces in `src/types/` — **never** use inline `any` casts for API data
- Use `const` for all variable declarations; use `let` only when reassignment is needed

## 2. Component Architecture

- **File naming**: PascalCase for components (`BookingCard.tsx`), camelCase for hooks (`useBookingList.ts`), kebab-case for utilities (`date-utils.ts`)
- **One component per file** — no barrel exports that re-export default exports
- Component structure order within a file:
  1. Imports
  2. Types/Interfaces
  3. Component function
  4. Export
- Keep components focused: if a component exceeds ~150 lines, extract sub-components or logic into custom hooks
- Use **React.memo** only when performance profiling shows unnecessary re-renders; do not apply speculatively

## 3. State Management

- Use React's built-in `useState`, `useReducer`, `useContext` for local/shared state
- Do **not** introduce external state management libraries (Redux, Zustand) without explicit justification
- Server state must be managed with a data-fetching library (or `useEffect` with proper cleanup), not duplicated in local state
- Never store derived values in state — compute them during render from primary state sources

## 4. API Communication

- All API calls must go through typed service functions in `src/services/` directory (e.g., `bookingService.ts`)
- Use the shared `axios` instance configured in `src/lib/axios.ts` — **never** instantiate `axios` directly in components
- Handle API errors in the service layer; surface them via consistent error objects
- All server timestamps are in ISO-8601 format; always parse to `Date` objects before displaying
- Include Keycloak access token in the `Authorization: Bearer <token>` header via the axios interceptor

## 5. Tailwind CSS v4 Usage

- Use Tailwind utility classes directly in JSX — **do not** write custom CSS unless Tailwind cannot achieve the result
- For repeated multi-class patterns, extract into a React component — **do not** use `@apply` as a workaround
- Maintain responsive design: always design mobile-first with breakpoints (`sm:`, `md:`, `lg:`)
- Color palette must follow the design tokens defined in the project — **never** use raw hex colors outside of config

## 6. Forms & Validation

- Use `react-hook-form` for all forms — **never** manage form field state manually with `useState`
- Define validation schemas with `zod` and integrate with `react-hook-form` via `@hookform/resolvers/zod`
- Display field-level error messages immediately below each input using the `FieldError` from `react-hook-form`
- Disable submit button while form is in a `isSubmitting` state

## 7. Date Handling

- Use `date-fns` for all date arithmetic and formatting — do not use `moment.js`
- Always display dates in Vietnamese locale (`vi`) using `date-fns/locale/vi`
- Date-only values (check-in, check-out) must be transmitted as `YYYY-MM-DD` strings to the API — never as timestamps

## 8. Accessibility (a11y)

- All interactive elements must be keyboard-navigable and have `aria-label` or visible text
- Images must have descriptive `alt` attributes (empty string `alt=""` for purely decorative images)
- Color contrast must meet WCAG 2.1 AA standard
- Use semantic HTML elements (`<button>`, `<nav>`, `<main>`, `<section>`) — **do not** attach click handlers to `<div>` elements

## 9. Performance

- Use `React.lazy` and `Suspense` for route-level code splitting
- Never import entire icon libraries — import individual icons only
- Avoid deeply nested `useEffect` dependencies; prefer `useMemo`/`useCallback` when passing stable references down
