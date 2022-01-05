# Offchain Bank

We describe an offchain bank application on top of the Ergo blockchain. At a high level, the bank maintains an offchain ledger 
of user balances as data of type (`pubKey`, `balance`), which are the (key, value) pairs to be stored in an AVL+ tree. 
An AVL+ tree is simply an AVL tree where the actual data is stored only at the leaf nodes. 
Each leaf in the AVL+ tree represents a user's balance in some tokens **T** that the bank issues.  

The bank performs the following operations offchain:
1. Transfer balance between two users by reducing one balance and increasing the other, ensuring that balances are always non-negative.
2. Remove leaf nodes from the tree, which amounts to users withdrawing their tokens from the bank. 
3. Add leaf nodes to the tree, with a possible non-zero balance. 

The sum of all the users balances is the total bank balance **x**.

At regular intervals, say every 1000 blocks, the bank publishes a box **B** along with **x** number of **T** tokens.
Each user is assumed to know the entire tree and can easily verify that the box is correct. 

If the bank disappears for a while and no update has been posted for 1000 blocks, the bank is considered **defunct**, and users
can directly withdraw the tokens from **B** by proving membership in the tree. 
During this operation, the user must add their public key to another **withdraw**-tree maintained in the same box. 
For such a withdrawal to be valid, users must also prove that the withdraw-tree does not contain their public key.

## Box structure

The bank box has the following structure:

Tokens:
1. Bank NFT to identify bank.
2. Token **T** in quantity **x**.

Registers:
1. R3: The creation height, which should be within an error margin of the actual mining height.
2. R4: Root hash of the bank ledger tree. 
3. R5: GroupElement (bank pub key).
4. R6: Boolean indicating if the bank is defunct.

Value: 
Minimum value to keep box from being garbage collected.

Script:
1. Creation height must be within an error margin of HEIGHT.
2. If current height is more than 1000 of creation height, box can be taken into **defunct**-mode by anyone who can prove membership in the tree. 
3. If the box is not in defunct-mode, bank public key can update: R3 (creation height), R4 (root of ledger) and balance of **T** tokens.
5. Once box is in defunct-mode, only withdraws are allowed as per the following rule.
4. During withdraw, user must prove membership in ledger tree as well as prove removal of the outgoing ledger tree.






