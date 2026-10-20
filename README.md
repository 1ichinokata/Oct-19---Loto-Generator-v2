# Japan Lottery Generator — Loto 6 + Loto 7 + Mini Loto

Combined Android Studio app for Japan's Loto 6, Loto 7 and Mini Loto.

## Loto 6
- 6 numbers, 1–43
- Previous 10 draws used for generation
- Latest draw excluded from analysis
- Latest-draw numbers explicitly blocked from generation
- Random, Balanced, Frequency, Hot and Cold strategies
- Odd/Even and Low/High filters
- 10 combinations, scoring and copy
- 30-draw history and previous-10 frequency analysis

## Loto 7
- 7 numbers, 1–37
- Previous 10 draws used for generation
- Latest draw excluded from analysis
- Latest-draw numbers explicitly blocked from generation
- Random, Balanced, Frequency, Hot and Cold strategies
- Odd/Even and Low/High filters
- 10 combinations, scoring and copy
- 30-draw history and previous-10 frequency analysis

## Mini Loto
- 5 numbers, 1–31
- Previous 10 draws used for generation
- Latest draw excluded from analysis
- Latest-draw numbers explicitly blocked from generation
- Random, Balanced, Frequency, Hot and Cold strategies
- Odd/Even and Low/High filters
- 10 combinations, scoring and copy
- 30-draw history and previous-10 frequency analysis

## Favorites
- Favorite tab added to each lottery
- Tap the heart on a generated combination to save it
- Favorites are stored persistently on the device
- Remove favorites with the delete button
- Copy saved combinations from the Favorites tab
- Favorites are separated between Loto 6, Loto 7 and Mini Loto

## Combined UI
- One Android app
- Switch between Loto 6 → Mini Loto → Loto 7 → Loto 6 from the top bar
- Shared Dark/Light mode setting
- Stable Material 3 UI; no ExperimentalMaterial3Api annotation
- Lottolyzer data source

## Build
Open the folder in Android Studio and build/run the app. Use JDK 17.

## Favorites latest-draw matching
- Favorite combinations compare each saved number with the current latest draw.
- Numbers that match the latest draw are highlighted with the Material error color.
- Favorites also show how many numbers matched the latest draw.
- Matching is handled separately for Loto 6, Loto 7, and Mini Loto.
