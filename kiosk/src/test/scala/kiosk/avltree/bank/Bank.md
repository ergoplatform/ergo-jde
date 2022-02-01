# Offchain Bank

We describe an offchain bank application on top of the Ergo blockchain. At a high level, the bank maintains an offchain ledger
of user balances as data of type (`pubKey`, `balance`), which are the (key, value) pairs to be stored in an AVL tree.
Each leaf is a pair (key, value), where the key is a user's public key and the value is the user's balance in some tokens **T** that the bank issues.

The bank performs the following operations offchain:
1. Transfer balance between two users by reducing one balance and increasing the other, ensuring that balances are always non-negative.
2. Remove leaf nodes from the tree, which amounts to users withdrawing their tokens from the bank.
3. Add leaf nodes to the tree, with a possible non-zero balance.

The sum of all the users balances is the total bank balance **x**.

At regular intervals, say every 1000 blocks, the bank publishes a box **B** along with **x** number of **T** tokens.
Each user is assumed to know the entire tree and can easily verify that the box is correct.

If the bank disappears for a while and no update has been posted for 1000 blocks, the bank is considered **defunct**, and users
can directly withdraw the tokens from **B** by proving membership in the tree.
During this operation, the user must prove that their public key exists in the ledger and then remove this key.

### Security Model

This model only protects against a disappearing bank and assumes that the on-chain data published by the bank is correct. The customers are supposed
to know the entire tree and can verify the correctness of the online digest at any time.

## Box structure

The bank box has the following structure:

Tokens:
1. Bank NFT to identify bank.
2. Token **T** in quantity **x** + 1.

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
3. At any time (irrespective of whether the box is defunct or not) the bank public key can update: R3 (creation height), R4 (root of ledger) and balance of **T** tokens. 
   After such an update, the bank box must not be defunct.
5. If the box is in defunct-mode, withdraws are allowed as per the following rule.
4. During withdraw, user must prove membership in the ledger tree and then remove their key from the tree

Full script is [here](Bank.scala), which corresponds to the address `AXCSJDXBy8jBKPQBzqKoVpwU2HQYk14ZrK8kL4JLfLogahTpwNcVJRRLitZ23GEpBF69Bi8Jw8bYs2MZxJ9NeJJFH1uvC1cbA9en9daJguveU1EvF6rK9efb3MMKRrx3R692jt4wSTaMJxcjLapT51r4BWUu8wzh55xFAgKs4MtWqGGVb64dPFskFiJ6w3bP8GmLfkatWpxwPSHGY1u6bJZzFqRctftyQe2rxDyznH4wnfwzmFAefFhJhMcPsibob1AEsvugxtDPgG9EYmLX8nXbDRBgWphHKgQ86pr3Ud4rBmhSWApcbfbs7KHFqcuhfKNuXZFsysLdh6dwGVisEKTPhheM9UyfULm9kD474PnSk4DHpEHtgx2ip3wNJLbV3Wu6XXGSJsHLrRstFhpPqUak7tqzJDc8K8SmNticAmdBRKCyJoL` as can be seen [here](https://wallet.plutomonkey.com/p2s/?source=IC8vIHRoaXMgYm94CiAvLyBSNCByb290IGhhc2ggKENvbGxbQnl0ZV0pCiAvLyBSNSBiYW5rIHB1YiBrZXkgKEdyb3VwRWxlbWVudCkKIC8vIFI2IEludCAoaWYgPT0gMCBpbmRpY2F0ZXMgYmFuayBpcyBub3QgZGVmdW5jdCkKIAogLy8gdG9rZW5zKDApID0gYmFua05GVAogLy8gdG9rZW5zKDEpID0gYmFuayBpc3N1ZXMgdG9rZW5zCiB7CiAgICB2YWwgdGltZU91dCA9IDUKICAgIAogICAgdmFsIGluQ3JlYXRpb25IZWlnaHQgPSBTRUxGLmNyZWF0aW9uSW5mby5fMQogICAgdmFsIGluTGVkZ2VyVHJlZSA9IFNFTEYuUjRbQXZsVHJlZV0uZ2V0CiAgICB2YWwgaW5CYW5rUHViS2V5ID0gU0VMRi5SNVtHcm91cEVsZW1lbnRdLmdldAogICAgdmFsIGluSXNEZWZ1bmN0ID0gU0VMRi5SNltJbnRdLmdldCAhPSAwCiAgICAKICAgIHZhbCBvdXQgPSBPVVRQVVRTKDApCiAgICAKICAgIHZhbCBvdXRDcmVhdGlvbkhlaWdodCA9IG91dC5jcmVhdGlvbkluZm8uXzEKICAgIHZhbCBvdXRMZWRnZXJUcmVlID0gb3V0LlI0W0F2bFRyZWVdLmdldAogICAgdmFsIG91dEJhbmtQdWJLZXkgPSBvdXQuUjVbR3JvdXBFbGVtZW50XS5nZXQgLy8ganVzdCBhY2Nlc3MgaXQgdG8gZW5zdXJlIHRoZXJlIGlzIGEgZ3JvdXAgZWxlbWVudDsgbm8gbmVlZCB0byB2YWxpZGF0ZSBpZiBpdHMgc2FtZQogICAgdmFsIG91dElzRGVmdW5jdCA9IG91dC5SNltJbnRdLmdldCAhPSAwCiAgICAgCiAgICB2YWwgdmFsaWRTdWNjZXNzb3IgPSB7CiAgICAgIG91dC5wcm9wb3NpdGlvbkJ5dGVzID09IFNFTEYucHJvcG9zaXRpb25CeXRlcyAmJgogICAgICBvdXQudmFsdWUgPj0gMTAwMDAwICAgICAgICAgICAgICAgICAgJiYKICAgICAgb3V0LnRva2VucygwKSA9PSBTRUxGLnRva2VucygwKSAgICAgICAgICAgICAgICYmCiAgICAgIG91dC50b2tlbnMoMSkuXzEgPT0gU0VMRi50b2tlbnMoMSkuXzEKICAgIH0KICAgIAogICAgdmFsIHZhbGlkQmFua1NwZW5kID0gewogICAgICAhIG91dElzRGVmdW5jdCAgICAgICAgICAgICAgICAgICYmCiAgICAgIHByb3ZlRGxvZyhpbkJhbmtQdWJLZXkpICAgICAgICAgJiYgCiAgICAgIG91dENyZWF0aW9uSGVpZ2h0ID4gSEVJR0hUIC0gMTAgCiAgICB9CiAgICAKICAgIHZhbCBtYWtlRGVmdW5jdCA9IHsKICAgICAgISBpbklzRGVmdW5jdCAgICAgICAgICAgICAgICAgICAgICAgICYmCiAgICAgIG91dElzRGVmdW5jdCAgICAgICAgICAgICAgICAgICAgICAgICAmJgogICAgICBpbkNyZWF0aW9uSGVpZ2h0IDwgSEVJR0hUIC0gdGltZU91dCAgJiYKICAgICAgb3V0TGVkZ2VyVHJlZSA9PSBpbkxlZGdlclRyZWUgICAgICAgICYmCiAgICAgIG91dC50b2tlbnMgPT0gU0VMRi50b2tlbnMgICAgICAgICAKICAgIH0KICAgIAogICAgdmFsIGlzV2l0aGRyYXcgPSB7CiAgICAgIHZhbCB3aXRoZHJhd0JveCA9IE9VVFBVVFMoMSkKICAgICAgdmFsIHdpdGhkcmF3VG9rZW5JZCA9IHdpdGhkcmF3Qm94LnRva2VucygwKS5fMQogICAgICB2YWwgd2l0aGRyYXdWYWx1ZSA9IHdpdGhkcmF3Qm94LnRva2VucygwKS5fMgogICAgICB2YWwgd2l0aGRyYXdLZXkgPSBibGFrZTJiMjU2KHdpdGhkcmF3Qm94LnByb3Bvc2l0aW9uQnl0ZXMpCgogICAgICB2YWwgcmVtb3ZlUHJvb2YgPSB3aXRoZHJhd0JveC5SNFtDb2xsW0J5dGVdXS5nZXQKICAgICAgdmFsIGxvb2t1cFByb29mID0gd2l0aGRyYXdCb3guUjRbQ29sbFtCeXRlXV0uZ2V0CiAgICAgIAogICAgICB2YWwgd2l0aGRyYXdBbXRDb2xsQnl0ZSA9IGluTGVkZ2VyVHJlZS5nZXQod2l0aGRyYXdLZXksIGxvb2t1cFByb29mKS5nZXQKICAgICAgCiAgICAgIHZhbCB1c2VyQmFsYW5jZSA9IGJ5dGVBcnJheVRvTG9uZyh3aXRoZHJhd0FtdENvbGxCeXRlKQogICAgICAKICAgICAgdmFsIHJlbW92ZWRUcmVlID0gaW5MZWRnZXJUcmVlLnJlbW92ZShDb2xsKHdpdGhkcmF3S2V5KSwgcmVtb3ZlUHJvb2YpLmdldAogICAgICAgIAogICAgICB2YWwgY29ycmVjdEFtb3VudCA9IHdpdGhkcmF3VmFsdWUgPT0gdXNlckJhbGFuY2UKICAgICAgdmFsIGNvcnJlY3RCYWxhbmNlID0gb3V0LnRva2VucygxKS5fMiA9PSBTRUxGLnRva2VucygxKS5fMiAtIHdpdGhkcmF3VmFsdWUgCiAgICAgIHZhbCBjb3JyZWN0VG9rZW5JZCA9IHdpdGhkcmF3VG9rZW5JZCA9PSBTRUxGLnRva2VucygxKS5fMSAKICAgICAgCiAgICAgIGluSXNEZWZ1bmN0ICAgICAgICAgICAgICAgICAgJiYgCiAgICAgIG91dElzRGVmdW5jdCAgICAgICAgICAgICAgICAgJiYKICAgICAgcmVtb3ZlZFRyZWUgPT0gb3V0TGVkZ2VyVHJlZSAmJgogICAgICBjb3JyZWN0QW1vdW50ICAgICAgICAgICAgICAgICYmCiAgICAgIGNvcnJlY3RCYWxhbmNlICAgICAgICAgICAgICAgJiYKICAgICAgY29ycmVjdFRva2VuSWQKICAgIH0KICAgIAogICAgc2lnbWFQcm9wKCh2YWxpZEJhbmtTcGVuZCB8fCBtYWtlRGVmdW5jdCB8fCBpc1dpdGhkcmF3KSAmJiB2YWxpZFN1Y2Nlc3NvcikKIH0K) 

The bank box has been successfully tested (spent) for [withdrawal](https://explorer.ergoplatform.com/en/transactions/260dbad30193e795c9ae612f598f311461fcee03948749fe0db72d625cfeae49) and [update](https://explorer.ergoplatform.com/en/transactions/099e6ca0b309432516d5ca8dbcb20dff3fc3e2d6bbc54fa24dfd19d4582ad83f) operations using [this](WithdrawSpec.scala) and [this](UpdateRootSpec.scala) example.




