# AVL Trees in Ergo

Ergo has native support for AVL trees, an efficient authenticated data structure.
An authenticated data structure allows proving various properties of the data without needing to know the entire data. 
This article describes how to use AVL trees in Ergo smart contracts.

## What are AVL Trees?

An [AVL tree](https://en.wikipedia.org/wiki/AVL_tree), named after its inventors (Adelson-Velsky and Landis) is a binary search tree 
with certain properties that enable efficient lookups, insertions and deletions.
In our context, the AVL tree is used as a Merkle tree: a root digest authenticates the entire data structure. 

Without going into the details of AVL trees (for which we refer the reader to the paper *[Improving Authenticated Dynamic Dictionaries,
with Applications to Cryptocurrencies](https://eprint.iacr.org/2016/994.pdf)*), we describe how AVL trees can be used in smart contracts.
However, a few things are worth noting:
1. Ergo uses a variant of AVL trees where the data is stored only in leaf nodes and intermediate nodes store information only for assisting search.
2. AVL trees are sorted, so given any data, it is easy to prove both existence and non-existence of data in a tree.
3. The leaves of the tree contain data in the form of (key, value) pairs. Both key and value are of type `Coll[Byte]`. 
4. The sorting is done only by the key, while the entire data is used in computing the digests.
5. Given an AVL tree, we can do following operations:
   
    1. Lookup a key and return the corresponding value as an `Option` type, that is, `Some(value)` if the key exists or `None` if  it does not. 
    2. Insert a (key, value) pair as a leaf node and obtain the new root digest.
    3. Delete a key (and the corresponding value) and obtain the new root digest.
     
## AVL Trees in Smart Contracts

The above operations (*Lookup*, *Insert* and *Delete*) can be performed inside a smart contract by supplying a corresponding *proof*.
The proofs are quite compact - for a tree with *n* leaves, the proofs are *O*(*log*(*n*)).

### Insert

For an *Insert* operation to be valid, the following is needed:

1. The root digest before insertion.
2. The (key, value) pair to be inserted.   
3. The root digest after insertion.
4. A proof that the root digest after inserting (key, value) is correctly derived from the root digest before insertion.
   The proof implicitly also proves that the key didn't exist in the tree earlier (an exception is thrown if it did).

### Lookup

A proof of *Lookup* needs:
1. The root digest.
2. The key to look for.
3. A proof of either existence or non-existence of the key. Both proofs have an identical structure.
       
   If the proof is correct then the returned object will be an `Option[Coll[Byte]]`, which will be `None` if the key does not exist 
   or will be `Some(value)` if it does. 
       
   If the proof is incorrect, an exception will be thrown instead. 

### Remove

A proof of *Remove* needs:
1. The root digest before removal.
2. The key to be removed.
3. The root digest after removal.
4. A proof that the root digest after removing the key is correctly derived from the root digest before removal.
   The proof implicitly also proves that the key existed in the tree earlier (an exception is thrown if it didn't). 

## Why use AVL Trees?

Since the proofs of AVL trees are very compact, we can perform operations on very large trees via very small transactions which only contain the 
proofs, root digest(s), and modified keys/values.

This enables many off-chain use-cases. For instance, a bank can store user balances in an AVL tree off-chain and periodically
update the root digest on-chain along with the corresponding total balance in tokens. In case the bank "disappears" after some time,
the customers can use the on-chain data to recover their balances in a trust-less manner. Note that the model only protects
against a disappearing bank and assumes that the on-chain data published by the bank is correct. 
This is described in detail [here](bank/Bank.md).

## Smart Contract Examples

We describe AVL Tree usage via very simple examples in ErgoScript. 
Note that ErgoScript encodes an AVL tree in an `AvlTree` type, which is essentially the root digest along with parameters such as key and value lengths.

### Insert 

This demonstrates how to perform an insert operation in ErgoScript. 

The smart contract requires the following parameters in the first output's registers:

- R4: The root digest before insertion, given as an `AvlTree`.  
- R5: The root digest after insertion, given as an`AvlTree`.
- R6: The key to be inserted, given as a `Coll[Byte]`.
- R7: The value to be inserted, given as a `Coll[Byte]`.
- R8: The proof of correct insertion, given as a `Coll[Byte]`.

The box can be spent if the following conditions hold:
1. The first output has an identical script as this box with some min number of ergs.
2. The proof correctly validates the new root digest after insertion of (key, value) to old root digest.

The following is the ErgoScript code for the [contract](ops/AvlInsert.scala):

```scala
{
   val selfOut = OUTPUTS(0)
  
   val inTree = selfOut.R4[AvlTree].get // root digest before insertion
   val outTree = selfOut.R5[AvlTree].get // root digest after insertion
   val key = selfOut.R6[Coll[Byte]].get // key to insert
   val value = selfOut.R7[Coll[Byte]].get // value to insert
   val proof = selfOut.R8[Coll[Byte]].get // proof of correct insertion
   
   val newTree = inTree.insert(Coll((key, value)), proof).get
   
   val validSpend = SELF.propositionBytes == selfOut.propositionBytes && 
                    outTree == newTree
                       
   sigmaProp(validSpend)
}
```

This corresponds to the address `2ma4cozJw8VuB2GmYXCeHr72R9w9S9kHynUJUrMV8u7DDuh5EVJ2iWmRagfh4CqeCGRch3p9Tvmq16Fr9xNsuD7geFMJur` as can be seen [here](https://wallet.plutomonkey.com/p2s/?source=eyAgCiAgIAogICB2YWwgc2VsZk91dCA9IE9VVFBVVFMoMCkKICAKICAgdmFsIGluVHJlZSA9IHNlbGZPdXQuUjRbQXZsVHJlZV0uZ2V0CiAgIHZhbCBvdXRUcmVlID0gc2VsZk91dC5SNVtBdmxUcmVlXS5nZXQKICAgdmFsIGtleSA9IHNlbGZPdXQuUjZbQ29sbFtCeXRlXV0uZ2V0CiAgIHZhbCB2YWx1ZSA9IHNlbGZPdXQuUjdbQ29sbFtCeXRlXV0uZ2V0CiAgIHZhbCBwcm9vZiA9IHNlbGZPdXQuUjhbQ29sbFtCeXRlXV0uZ2V0CiAgIAogICB2YWwgbmV3VHJlZSA9IGluVHJlZS5pbnNlcnQoQ29sbCgoa2V5LCB2YWx1ZSkpLCBwcm9vZikuZ2V0CiAgIAogICB2YWwgdmFsaWRTcGVuZCA9IFNFTEYucHJvcG9zaXRpb25CeXRlcyA9PSBzZWxmT3V0LnByb3Bvc2l0aW9uQnl0ZXMgJiYgCiAgICAgICAgICAgICAgICAgICAgb3V0VHJlZSA9PSBuZXdUcmVlCiAgICAgICAgICAgICAgICAgICAgICAgCiAgIHNpZ21hUHJvcCh2YWxpZFNwZW5kKQp9).

This box was [spent](https://explorer.ergoplatform.com/en/addresses/2ma4cozJw8VuB2GmYXCeHr72R9w9S9kHynUJUrMV8u7DDuh5EVJ2iWmRagfh4CqeCGRch3p9Tvmq16Fr9xNsuD7geFMJur) on the main-net using proofs generated via AppKit as described [here](ops/AvlInsertSpec.scala).

### Remove

This demonstrates how to perform a remove operation in ErgoScript.

The smart contract requires the following parameters in the first output's registers:

- R4: The root digest before removal, given as an `AvlTree`.
- R5: The root digest after removal, given as an`AvlTree`.
- R6: The key to be removed, given as a `Coll[Byte]`.
- R7: The proof of correct removal, given as a `Coll[Byte]`.

The box can be spent if the following conditions hold:
1. The first output has an identical script as this box with some min number of ergs.
2. The proof correctly validates the new root digest after deletion of key from the old root digest.

The following is the ErgoScript code for the [contract](ops/AvlRemove.scala):

```scala
{  
   val selfOut = OUTPUTS(0)
  
   val inTree = selfOut.R4[AvlTree].get // root digest before removal
   val outTree = selfOut.R5[AvlTree].get // root digest after removal
   val key = selfOut.R6[Coll[Byte]].get // key to remove
   val value = selfOut.R7[Coll[Byte]].get // not currently used but can be used to match value before removing 
   val proof = selfOut.R8[Coll[Byte]].get // proof that the key was removed
   
   val newTree = inTree.remove(Coll(key), proof).get
   
   val validSpend = SELF.propositionBytes == selfOut.propositionBytes && 
                    outTree == newTree
                       
   sigmaProp(validSpend)
}
```

This corresponds to the address `88dajuYgRcAeikGQY5izJR1qQVkn7TQudPcM7NVWFiE2rudRyMVqHc1CdVywshEYLfBmgZJtwRDzkJo2` as can be seen [here](https://wallet.plutomonkey.com/p2s/?source=eyAgCiAgIAogICB2YWwgc2VsZk91dCA9IE9VVFBVVFMoMCkKICAKICAgdmFsIGluVHJlZSA9IHNlbGZPdXQuUjRbQXZsVHJlZV0uZ2V0CiAgIHZhbCBvdXRUcmVlID0gc2VsZk91dC5SNVtBdmxUcmVlXS5nZXQKICAgdmFsIGtleSA9IHNlbGZPdXQuUjZbQ29sbFtCeXRlXV0uZ2V0CiAgIHZhbCB2YWx1ZSA9IHNlbGZPdXQuUjdbQ29sbFtCeXRlXV0uZ2V0CiAgIHZhbCBwcm9vZiA9IHNlbGZPdXQuUjhbQ29sbFtCeXRlXV0uZ2V0CiAgIAogICB2YWwgbmV3VHJlZSA9IGluVHJlZS5yZW1vdmUoQ29sbChrZXkpLCBwcm9vZikuZ2V0CiAgIAogICB2YWwgdmFsaWRTcGVuZCA9IFNFTEYucHJvcG9zaXRpb25CeXRlcyA9PSBzZWxmT3V0LnByb3Bvc2l0aW9uQnl0ZXMgJiYgCiAgICAgICAgICAgICAgICAgICAgb3V0VHJlZSA9PSBuZXdUcmVlCiAgICAgICAgICAgICAgICAgICAgICAgCiAgIHNpZ21hUHJvcCh2YWxpZFNwZW5kKQp9).

This box was [spent](https://explorer.ergoplatform.com/en/transactions/795c32aaade3ff4a0814db9e7d69b28cb2714c2796a68250a9cdf73b75dc4957) on the main-net using proofs generated via AppKit as described [here](ops/AvlRemoveSpec.scala).

### Lookup (Key Exists)

This demonstrates how to perform a lookup operation in ErgoScript, provided that the key exists.

The smart contract requires the following parameters in the first output's registers:

- R4: The root digest, given as an `AvlTree`.
- R5: The key to be looked up, given as a `Coll[Byte]`.
- R6: The value that is expected to be found, given as a `Coll[Byte]`.
- R7: The proof of correct lookup, given as a `Coll[Byte]`.

The box can be spent if the following conditions hold:
1. The first output has an identical script as this box with some min number of ergs.
2. The proof correctly validates the lookup of key for the root digest.

The following is the ErgoScript code for the [contract](ops/AvlLookup.scala):

```scala
{
   val selfOut = OUTPUTS(0)

   val tree = selfOut.R4[AvlTree].get  // root digest
   val key = selfOut.R5[Coll[Byte]].get  // key to lookup
   val value = selfOut.R6[Coll[Byte]].get  // to match with looked up value
   val proof = selfOut.R7[Coll[Byte]].get  // proof that the key exists

   val found = tree.get(key, proof).get

   val validSpend = SELF.propositionBytes == selfOut.propositionBytes &&
                    found == value

   sigmaProp(validSpend)
}
```

This corresponds to the address `5p12RmDtsNmgCBxJGkGog5HtHMH9CZuEpvh82KRMNxcbSA6WhKWuh9CjbmDb62z1EGYujiKHYUSU` as can be seen [here](https://wallet.plutomonkey.com/p2s/?source=eyAgCiAgIAogICB2YWwgc2VsZk91dCA9IE9VVFBVVFMoMCkKICAKICAgdmFsIHRyZWUgPSBzZWxmT3V0LlI0W0F2bFRyZWVdLmdldAogICB2YWwga2V5ID0gc2VsZk91dC5SNVtDb2xsW0J5dGVdXS5nZXQKICAgdmFsIHZhbHVlID0gc2VsZk91dC5SNltDb2xsW0J5dGVdXS5nZXQKICAgdmFsIHByb29mID0gc2VsZk91dC5SN1tDb2xsW0J5dGVdXS5nZXQKICAgCiAgIHZhbCBmb3VuZCA9IHRyZWUuZ2V0KGtleSwgcHJvb2YpLmdldAogICAKICAgdmFsIHZhbGlkU3BlbmQgPSBTRUxGLnByb3Bvc2l0aW9uQnl0ZXMgPT0gc2VsZk91dC5wcm9wb3NpdGlvbkJ5dGVzICYmIAogICAgICAgICAgICAgICAgICAgIGZvdW5kID09IHZhbHVlCiAgICAgICAgICAgICAgICAgICAgICAgCiAgIHNpZ21hUHJvcCh2YWxpZFNwZW5kKQp9).

This box was [spent](https://explorer.ergoplatform.com/en/transactions/083fd0e35ea4706e53f80c29708f3c5337992ac7b4188f0dbcdd3d9597d54d45) on the main-net using proofs generated via AppKit as described [here](ops/AvlLookupSpec.scala).

### Lookup (Does Not Exist)

This demonstrates how to perform lookup operation in ErgoScript, provided that the key does not exist.

The smart contract requires the following parameters in the first output's registers:

- R4: The root digest, given as an `AvlTree`.
- R5: The key to be looked up, given as a `Coll[Byte]`.
- R6: The proof of correct non-existent lookup, given as a `Coll[Byte]`.

The box can be spent if the following conditions hold:
1. The first output has an identical script as this box with some min number of ergs.
2. The proof correctly validates the non-existent lookup of key for the root digest.

The following is the ErgoScript code for the [contract](ops/AvlNotExist.scala):

```scala
{  
   val selfOut = OUTPUTS(0)
  
   val tree = selfOut.R4[AvlTree].get   // root digest
   val key = selfOut.R5[Coll[Byte]].get  // key to lookup
   val proof = selfOut.R6[Coll[Byte]].get // proof that the key does not exist
   
   val notExist = tree.get(key, proof).isDefined == false // if proof is invalid then this will throw exception
   
   val validSpend = SELF.propositionBytes == selfOut.propositionBytes && 
                    notExist
                       
   sigmaProp(validSpend)
}
```

This corresponds to the address `3BwMr185rGvYAttAPpChRv39vhYUk19A6LQW6irAfYMKqZSmJpwG8Vbxbqv5h25UGQVm` as can be seen [here](https://wallet.plutomonkey.com/p2s/?source=eyAgCiAgIAogICB2YWwgc2VsZk91dCA9IE9VVFBVVFMoMCkKICAKICAgdmFsIHRyZWUgPSBzZWxmT3V0LlI0W0F2bFRyZWVdLmdldAogICB2YWwga2V5ID0gc2VsZk91dC5SNVtDb2xsW0J5dGVdXS5nZXQKICAgdmFsIHByb29mID0gc2VsZk91dC5SNltDb2xsW0J5dGVdXS5nZXQKICAgCiAgIHZhbCBub3RFeGlzdCA9IHRyZWUuZ2V0KGtleSwgcHJvb2YpLmlzRGVmaW5lZCA9PSBmYWxzZQogICAKICAgdmFsIHZhbGlkU3BlbmQgPSBTRUxGLnByb3Bvc2l0aW9uQnl0ZXMgPT0gc2VsZk91dC5wcm9wb3NpdGlvbkJ5dGVzICYmIAogICAgICAgICAgICAgICAgICAgIG5vdEV4aXN0CiAgICAgICAgICAgICAgICAgICAgICAgCiAgIHNpZ21hUHJvcCh2YWxpZFNwZW5kKQp9).

This box was [spent](https://explorer.ergoplatform.com/en/transactions/c9c7f16a754065b3d2478a693d96ead241eada03d3222751c703befca305a4f2) on the main-net using proofs generated via AppKit as described [here](ops/AvlNotExistSpec.scala).
