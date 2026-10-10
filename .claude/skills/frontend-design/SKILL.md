---
name: frontend-design
description: Visual design, layout and UX rules for the BidAndDrive frontend: auction listings, bid panel, status states, responsive layout, accessibility. Use when creating or changing any screen, layout, component styling or user-facing copy in frontend/.
---

# Frontend design

Patterns are taken from car and general auction sites (Bring a Trailer, Cars & Bids, eBay Motors, Catawiki). Borrow the patterns, never their branding, colours or copy.

Read `frontend/CLAUDE.md` and the `vue3-conventions` skill first. This skill covers what the user sees. `vue3-conventions` covers how the code is written.

## Principles
1. **The bid is the hero.** On an auction page, the current price, time left and bid action are always visible without scrolling, on desktop and mobile.
2. **Status is never implicit.** Every auction shows exactly one state: `scheduled`, `live`, `ended`, `sold`, `reserve not met`. Every buyer sees one personal state when signed in: `winning`, `outbid`, `no bids yet`, `watching`.
3. **Money and time are exact.** Show currency with two decimals and a fixed currency code. Show time as a countdown under 24 hours and as a date otherwise. Show the timezone of the auction's end time.
4. **Plain language.** Use "Place bid", "Your bid must be at least €X", "Auction ended on …". No jargon, no exclamation marks.
5. **Errors say what to do.** "Bid must be higher than €1,200. Enter €1,250 or more." Never "Invalid input".

## Layout
- Listing grid: cards with one photo, title, current price, time left and a status badge. Use CSS grid with `repeat(auto-fill, minmax(16rem, 1fr))`.
- Auction detail: two columns on desktop (photo gallery and specs left, bid panel right). Stack the bid panel directly under the gallery on mobile, with the bid button sticky at the bottom.
- Bid panel contents, in order: status, current highest bid, minimum next bid, time left, bid input, primary "Place bid" button, bid history.
- Bid history: newest first, showing amount, time and bidder display name. Never show emails or full names of other bidders.
- Seller dashboard: a table of the seller's auctions with columns for status, current bid, bid count and end time. It collapses to a stacked card list under 640px.

## Components and states
- Every async area has three states: loading (skeleton, not a spinner over blank space), empty (a sentence and one next action), and error (message and a retry button).
- Disable the bid button while a request is pending. Show a pending state on the button and keep the input value.
- After a successful bid, show a short inline confirmation. Do not use a blocking modal.
- A bid that loses a race shows "Another bid was placed first. The current bid is now €X" and updates the panel.

## Visual system
- Use CSS custom properties for colours, spacing and type scale on `:root`. Components use tokens, never raw hex values.
- Spacing scale: 4, 8, 12, 16, 24, 32, 48 px. Do not add values outside the scale.
- One accent colour for the primary action. Reserve red for destructive or failed states, and green for "winning" only.
- Body text at least 16px. Line length for descriptions capped at about 70 characters.

## Accessibility (non-negotiable)
- Contrast at least WCAG AA: 4.5:1 for body text, 3:1 for large text and UI controls.
- Every input has a visible label. Errors are linked with `aria-describedby` and announced with `role="alert"` only when they appear after a submit.
- The countdown updates at most once per second and is not announced by screen readers on every tick. Announce only "ends in 5 minutes" thresholds.
- All actions work with the keyboard. Focus rings are visible. Never remove `outline` without a replacement.
- Images have alt text. Decorative images use `alt=""`.

## Responsive rules
- Test at 375px, 768px and 1280px. No horizontal scroll at any of them.
- Touch targets at least 44 by 44 px.
- No fixed heights on cards or panels. Let content wrap.

## Checklist before a screen is done
- Status, current bid and time left visible on first paint.
- Loading, empty and error states exist.
- Keyboard-only walkthrough completed.
- Contrast checked on the new colours.
- Works at 375px wide.
- Copy follows the plain-language rules above.
