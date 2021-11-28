# Dexy Stablecoin Design

This gives a high level overview of the Dexy contracts. The design is extended from the forum post by Kushti.

Below are the main design aspects.

1. There is an emission contract that emits Dexy tokens (example DexyUSD) in a one-way swap using the oracle pool rate. 
The swap is one-way in the sense that we can only buy Dexy tokens by selling ergs to the box. We cannot do the reverse swap. 
   
2. The reverse swap, selling of Dexy tokens, is done via a Liquidity Pool (LP) which also permits buying Dexy tokens. The LP 
   primarily uses the logic of Uniswap V2. The difference is that the LP also takes as input the oracle pool rate and uses that to modify certain logic. In particular,
   redeeming of LP tokens is not allowed when the oracle pool rate is below a certain percent (say 90%) of the oracle pool rate.
   
3. In case the oracle pool rate is higher than LP rate, then traders can do arbitrage by buying Dexy tokens from the emission box and 
   selling them to the LP. 
   
4. In case the oracle pool rate is lower than LP rate, then the Ergs collected in the emission box can be used to bring the rate back up by performing a swap.
   We call this the "top-up swap".
   
The swap logic is encoded in a **swapping** contract.

There is another contract, the **tracking** contract that is responsible for tracking the LP's state. In particular, this contract
tracks the block at which the "top-up-swap" is initiated. The swap can be initiated when the LP rate falls below 90%.
Once initiated, if the LP rate remains below the oracle pool rate for a certain threshold number of blocks, the swap can be compleded.
On the other hand, if before the threshold the rate goes higher than oracle pool then the swap must be aborted.

The LP uses a "cross-counter" to keep count of the number of times the LP rate has crossed the oracle pool rate (from below or above) in a swap transaction.
If the cross-counter is preserved at swap initiation and completion then swap is valid, else it is aborted. This logic is present in the swapping box. 