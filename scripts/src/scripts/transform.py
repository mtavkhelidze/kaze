import csv
import glob


def transform_football_data(input_pattern, output_file):
    # Target schema: div, date, team, goals
    header = ['div', 'date', 'team', 'goals']

    with open(output_file, 'w', newline='') as f_out:
        writer = csv.writer(f_out)
        writer.writerow(header)

        for file_path in glob.glob(input_pattern):
            with open(file_path, 'r', encoding='utf-8-sig') as f_in:
                reader = csv.DictReader(f_in)
                for row in reader:
                    # Row 1: Home Team
                    writer.writerow([
                        row['Div'], row['Date'], row['HomeTeam'], row['FTHG']
                    ])
                    # Row 2: Away Team
                    writer.writerow([
                        row['Div'], row['Date'], row['AwayTeam'], row['FTAG']
                    ])
