# Dexy Stablecoin Design

This gives a high level overview of the Dexy contracts. The design is extended from the forum post by Kushti.

Below are the main design aspects.

1. **One-way tethering**: There is a minting (or "emission") contract that emits Dexy tokens (example DexyUSD) in a one-way swap using the oracle pool rate.
The swap is one-way in the sense that we can only buy Dexy tokens by selling ergs to the box. We cannot do the reverse swap. 
   
2. **Liquidify Pool**: The reverse swap, selling of Dexy tokens, is done via a Liquidity Pool (LP) which also permits buying Dexy tokens. The LP 
   primarily uses the logic of Uniswap V2. The difference is that the LP also takes as input the oracle pool rate and uses that to modify certain logic. In particular,
   redeeming of LP tokens is not allowed when the oracle pool rate is below a certain percent (say 90%) of the LP rate.
   
3. In case the oracle pool rate is higher than LP rate, then traders can do arbitrage by minting Dexy tokens from the emission box and 
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

## Draining Attack

Consider the following attack:

1. When profitable, mint some Dexy tokens and sell them on LP
2. When not profitable, top up LP using Dexy till it becomes profitable
3. Go to step 1 

The process will repeat until the emission box runs out of Ergs, and the attacker would have made profit.

In order to stop this attack, we can look at one or more of the following measures

1. Lock the minted Dexy tokens in a box until a certain time
2. Lock the Ergs in the emission contract until a certain time
3. Don't allow minting when its profitable

kushti khushi, [23/01/22 11:49 PM]
On Dexyusd, I guess the biggest problem is that the most profitable strategy could be to mint usd at the top and put them on hold to drain erg from the pool then

kushti khushi, [23/01/22 11:50 PM]
So usd is short instrument against bank reserves then

kushti khushi, [23/01/22 11:51 PM]
And bank reserve is not overcollateraized even unlike sigusd

kushti khushi, [24/01/22 12:31 AM]
However, we have a delay for bank intervention to get started

kushti khushi, [24/01/22 4:40 AM]
And I guess in general a stablecoin is a short against reserves, if there are any reserves in place
 
kushti khushi, [24/01/22 8:00 PM]
Maybe we need to time lock minted usd if current price is above avg minting price, or require to put minted usd into the pool if the condition is met

kushti khushi, [24/01/22 8:00 PM]
Any thoughts?

kushti khushi, [27/01/22 1:24 AM]
Maybe we need to require minted USD to be put into the pool, if oracle ERG/USD price is way above avg minting price, with liquidity tokens to be time-locked for e.g. one day

kushti khushi, [21/02/22 5:18 AM]
@keitodot welcome, will add Luivatra and few other folks as well

kushti khushi, [21/02/22 5:20 AM]
@keitodot what kind of help do you need in regards with lending contracts?

k.吉, [21/02/22 5:30 AM]
For single lender uncollaterized, I’m only afraid of security. Other than that it should be good. And having sigusd or other tokens implemented would be a breeze (once I get my computer, battery bulging for my current one lol)

k.吉, [21/02/22 5:31 AM]
After that I’m planning to get collaterized loans in. And then pooled loans

kushti khushi, [21/02/22 5:32 AM]
Do you have contracts publushed I guess?

k.吉, [21/02/22 5:49 AM]
For the single lender ergs we have it published

k.吉, [21/02/22 5:49 AM]
https://github.com/Ergo-Lend/ergo-lend-documentation/blob/main/contracts/SingleLender/contracts.scala

kushti khushi, [26/02/22 3:03 AM]
[In reply to k.吉]
Thanks, will check over the weekend