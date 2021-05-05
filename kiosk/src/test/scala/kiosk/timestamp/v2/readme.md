## v2 (No address check)

In v1, to validate a timestamp, we had to check two conditions on the timestamp box: 
1. It has the address `4MQyMKvMbnCJG3aJ`.
2. It has exactly one timestamp token.

For simplicity, we would have just preferred to check that it has one timestamp token. This version allows this.
Essentially, it requires that every output timestamp-emission box has at least 2 tokens. Therefore, if a box has one timestamp token,
then it must be the timestamp box.

#### Differences with v1

- Output timestamp emission box must have **at least two** timestamp tokens.
- To be considered a valid timestamp, the timestamp box must have **exactly one** timestamp token.