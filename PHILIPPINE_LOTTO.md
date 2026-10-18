# Philippine Lotto integration

Added to the `philippine-lotto` branch:

- Lotto 6/42
- Mega Lotto 6/45
- Super Lotto 6/49
- Grand Lotto 6/55
- Ultra Lotto 6/58

Features:
- PCSO result retrieval layer
- Latest result and history
- Previous 10 draws for analysis
- Random, Balanced, Frequency, Hot and Cold generation
- Odd/Even and Low/High filters
- 10 generated combinations
- 0–100 scoring
- Favorites and manual Add
- Target draw date assignment

The existing AccessManager/Supabase protection and Japan lottery modules are preserved.

Note: PCSO's results page is a server-rendered form, so the result parser is intentionally tolerant of changes to the page markup.

Packaging workflow initialized.

Package workflow path fixed.


Results source changed to Lottolyzer history pages for all five Philippine games.

Rebuild requested after Lottolyzer source change.
