# Overview

The goal of this project is not to deliver a full-fledged application but to give an insight into how I would go about 
creating this project given a 4-hour limit. We've cut some corners and there are definitely areas we can improve in
but given the challenge to complete this within 4 hours, we hope the reader will understand our decisions on where
we decided to cut corners.


## How To Run The Application

### Using the script

We're assuming you've already have docker installed on your computer.

Assuming you're at the root of the project directory, all you have to do is run:

```shell
./scripts/start_docker.sh
```
It will build and run the application in the current terminal. Open a new terminal to start making simple requests.

> Note: If you're using a m-series mac, we're set the platform as x86.  


### Manually

If you want to run it manually, you can essentially follow the steps inside the script but from the root directory:

```shell
./gradlew build
```

This will create the jar file.

```shell
docker-compose up
```
Then just run docker-compose up.

## Data

We've only inserted two accounts into `accounts`.

They each have `$10,0000`. So you can make transfers between the two accounts. 
These two accounts should just be account id 1 and 2. We chose to just use int ids but in production, **this is a bad idea**.
We just chose to keep things simple for now. Ideally, I should have added an `account_id` field but 
decided to just keep things simple for now.


> Note: The amount is the smallest current unit. Example: 100 is not $100 but $1.00.

## APIs

### Getting an account

```markdown
curl --location 'localhost:8080/accounts/{id}'
```

Example:
```shell
curl --location 'localhost:8080/accounts/1'
```

### Create a transaction

> Note: We only accept USD for now to keep things simple.

```markdown
curl --location 'localhost:8080/accounts/transfer' \
--header 'Content-Type: application/json' \
--data '{
"fromAccount": ${FROM_ID},
"toAccount": ${TO_ID},
"amount": ${AMOUNT},
"uniqueId": ${IDEM_KEY},
"currency": ${CURRENCY}
}'
```

Example:
```shell
curl --location 'localhost:8080/accounts/transfer' \
--header 'Content-Type: application/json' \
--data '{
    "fromAccount": 1,
    "toAccount": 2,
    "amount": 500,
    "uniqueId": "40D064D2-7C96-4F6F-933B-A3324E5B7CCC",
    "currency": "USD"
}'
```


### Get transaction history

```markdown
curl --location 'localhost:8080/accounts/1/transfers?size=2&lastId=1'
```

| Field  | Description         |
|--------|---------------------|
 | size   | Limit               |
 | lastId | The last id you saw |

Example:

```shell
curl --location 'localhost:8080/accounts/1/transfers'
```

## Downsides, Where To Make Improvements

- Account id being just the primary key
  - This is bad from a security perspective. We should have added another field called "account_id", make this a unique column. 
- Currency just being USD
  - We decided to keep things simple to not worry to much about currency exchange rates
- Currency can be in its own table - normalization
  - The currency can be in its own data but to avoid joins, we decided to put it in their own tables.
- Creating a transaction has a unique id.
  - To make transfer request idempotent, we use added a unique id in the transaction table. Thus if, for some reason, the request times out, just re-do the same request. There are multiple ways to handle this but to keep things simple, we just left it in the transaction table.
- DB Isolation level is set to the default:  Read Committed
  - Since we're updating the balance with an update statement where we increment the balance amount via balance = balance + ${amount}, there's really no need for a stronger isolation level
- Trophy testing
  - Since we're on limited time, we focus on just integration tests: call the apis and make sure the response is correct.
- No negative account balance
  - To keep things simple, we require the account to have sufficient balance when making a transaction.
- Logging
  - We only do some simple logging. If it's an internal error, we log it. Any data integrity violation, we output a warning.
