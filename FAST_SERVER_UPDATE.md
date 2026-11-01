# v3.3 Fast Server Update

This version keeps the Server/cache behavior from v3.2 but makes refresh much faster.

## Changes
- Japan Loto 6, Loto 7, and Mini Loto now fetch the history page first and use one fallback page only.
- Japan games no longer wait for individual prize pages for every historical draw during a normal refresh.
- Philippine Lotto games use one history request with one Home-summary fallback instead of waiting through multiple history pages.
- Shared Server HTTP timeout reduced to 10 seconds.
- A slow/unavailable Server therefore fails faster and the existing saved cache can remain visible.
- Visible app wording remains "Server"; internal class names may still contain Lottolyzer.

Prize details may be unavailable after a fast refresh because they are no longer used to block the history update.
