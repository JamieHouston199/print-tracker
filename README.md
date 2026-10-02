# print-tracker

A Java console application for tracking 3D printer filament stock, print history and costs.

I built this to keep track of how much filament I have left on each spool and what each print actually costs me.

## Features

- **Filament inventory:** add spools by colour, cost per kg and weight. Adding a colour you already have tops up the existing spool instead of creating a duplicate.
- **Print logging:** record the model name, filament used and print time. Supports multicoloured prints with 2 to 4 filaments.
- **Automatic stock updates:** logging a print takes the grams used off the right spool, and warns you if the spool doesn't have enough left.
- **Cost tracking:** works out the cost of each print from the grams used and the price of each filament.
- **History and summary:** lists every print, with total filament used, total print time, total cost and average cost per print.
- **Saved between runs:** inventory and history are stored in CSV files, so nothing is lost when the program closes.

## Example

```
=== 3D Print Tracker ===
1. View filament inventory
2. Add new filament colour
3. Log a new print
4. View print history and summary
5. Remove a filament
6. Exit
Choose an option: 3
Model name: Phone stand
Is this print multicoloured? (y/n): n

Colour 1 of 1:
1. Black PLA - 850.0g left @ 0.0180/g
Choose a filament number: 1
Grams of Black PLA used: 42
Print time in hours: 2.5
Logged. Total cost: 0.76
```

## How to run

Open the project in Eclipse and run `PrintTracker.java`. It creates two CSV files in the project folder to save your data.

The program creates `filaments.csv` and `print_history.csv` in the project folder the first time you add a filament or log a print.

## How it works

- Data is held in parallel arrays, one set for filaments and one for print history, which grow or shrink as items are added or removed.
- Each CSV row is validated when the files are loaded, and invalid rows are skipped.
- When logging a print, every filament choice is collected before any stock is changed, so cancelling part-way through doesn't leave the inventory half-updated.

## Planned improvements

- Reject negative numbers for grams, cost and print time.
- Reject commas, colons and semicolons in names, which currently break the CSV files.
- Rewrite the parallel arrays as `Filament` and `Print` classes stored in `ArrayList`s.
- Record failed prints and the reason they failed.
