# Import time for sleep
import time
import psycopg2
import os

conn = psycopg2.connect(host=os.getenv('POSTGRES_SVC'),
    database="quarkus",
    user="quarkus",
    password="quarkus")

gap_query = """select key + 1 as gap_start, 
       next_nr - 1 as gap_end
from (
  select key, 
         lead(key) over (order by key) as next_nr
  from event
) nr
where key + 1 <> next_nr;"""

duplicate_query = """select * from (
select key, count(key) as c From event group by key) T
where c > 1"""

def print_gaps():
	# Open a cursor to perform database operations
	cur = conn.cursor()

	# Execute a query
	cur.execute(gap_query)

	# Retrieve query results
	records = cur.fetchall()

	if len(records) > 0:
		print("There are missing messages. Gaps: " + str(records))

	cur.close()

def print_duplicate():
	# Open a cursor to perform database operations
	cur = conn.cursor()

	# Execute a query
	cur.execute(duplicate_query)

	# Retrieve query results
	records = cur.fetchall()

	if len(records) > 0:
		print("Duplicate events: " + str(records))

	cur.close()

# While loop
while(True):
	print_gaps()
	print_duplicate()
	time.sleep(1)