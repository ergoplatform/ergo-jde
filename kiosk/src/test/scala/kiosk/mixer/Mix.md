Below is an idea for mixing coins motivated from ErgoMixer (aka zerojoin). Hence, some basic idea of ErgoMixer will be useful in understanding the motivation.

## Motivation
The primary motivation is to develop a better variant of ErgoMixer.

1. *Mix more than 2 boxes*: In ErgoMixer, we can mix only two boxes at a time and it will be good to have a solution that works for more than 2 boxes
2. *Make it more non-interactive*: In ErgoMixer, Bob can mix coin of Alice without interaction. However, to remix, Alice must be online. We want a variant where Alice's (and Bob's) box can be remixed multiple times without Alice's intervention.

## Basic idea

The following describes the high level idea for mixing two boxes at a time. The same idea can be trivially extended to mix three, four, etc boxes, which will not be described below.

The primary issue with ErgoMixer is that we cannot remix a full-mix box directly half the time; when playing Alice's role, we need to create a half-mix box, and for that we need to be online. Additionally, the half-mix box is a kind of "bloat". It will be good if we can operate with "full-mix" boxes only (called just "mix-boxes").

The idea is to start with two boxes having a script of type *proveDlog(\*, \*)* for the owners and have the mix process generate two boxes also of type *proveDlog(\*, \*)* for each owner.

**Note**: We are considering the generic variant of *proveDlog* that takes in two parameters: base and the value to take log of. Ergo's *proveDlog* has a fixed base *g* (the same as the one in Bitcoin's curve) and takes only one parameter. Ergo, however, has the *proveDHTuple* instruction that can be used to emulate what we want using the rule: *proveDlog(a, b) = proveDHTuple(a, a, b, b)*.

## Details

Since we have only one type of box, we will have only one type of participant, which we will call Alice.

At any time, a mix box has two registers used:
- R4 contains group element *h*, which is to be considered the base for discrete logs
- R5 contains group element *w*, which is the value whose discrete log is in question

The owner of the mix box has to prove the statement: *proveDlog(h, w) = proveDlog(R4, R5)*

To start, Alice (and many others) select a secret *x*, compute *w = g^x*.
Her first box is created with R4 = *g* and R5 = *w*.

At any time, a mix is done by selecting Alice's and another mix box along with a secret *y*. Then the transformation *(h, w) -> (h^y, w^y)* is applied to Alice's box (along with a similar transformation to the other box). The transformation preserves's Alice's secret exponent *x*, ensuring her ability to spend the box anytime. This is also called "re-randomising" the public key.

To ensure the transformation is correct, Alice must ensure that *y != 0* and that the new box preserves her secret exponent between R4 and R5. This is ensured in the rest of the contract of the mix box.
Note that just requiring R4 != R5 for the new boxes ensures that y != 0. The remaining part is handled by proveDHTuple. The [complete contract](https://wallet.plutomonkey.com/p2s/?source=eyAgCiAgIHZhbCBoID0gU0VMRi5SNFtHcm91cEVsZW1lbnRdLmdldCAgICAgLy8gY3VycmVudCBiYXNlIGZvciBkTG9nCiAgIHZhbCB3ID0gU0VMRi5SNVtHcm91cEVsZW1lbnRdLmdldAogICAKICAgdmFsIG93bmVyID0gcHJvdmVESFR1cGxlKGgsIGgsIHcsIHcpICAvLyA9IHByb3ZlRGxvZyhoLCB3KQogICAKICAgdmFsIG1peCA9IHsKICAgICB2YWwgb3V0MSA9IE9VVFBVVFMoMCkKICAgICB2YWwgb3V0MiA9IE9VVFBVVFMoMSkKICAgICAKICAgICB2YWwgaE91dDEgPSBvdXQxLlI0W0dyb3VwRWxlbWVudF0uZ2V0CiAgICAgdmFsIGhPdXQyID0gb3V0Mi5SNFtHcm91cEVsZW1lbnRdLmdldAogICAgIAogICAgIHZhbCB3T3V0MSA9IG91dDEuUjVbR3JvdXBFbGVtZW50XS5nZXQKICAgICB2YWwgd091dDIgPSBvdXQyLlI1W0dyb3VwRWxlbWVudF0uZ2V0CiAgICAgCiAgICAgdmFsIHZhbGlkT3V0cyA9IG91dDEucHJvcG9zaXRpb25CeXRlcyA9PSBTRUxGLnByb3Bvc2l0aW9uQnl0ZXMgJiYKICAgICAJICAgICAJICAgICBvdXQyLnByb3Bvc2l0aW9uQnl0ZXMgPT0gU0VMRi5wcm9wb3NpdGlvbkJ5dGVzICYmCiAgICAgCSAgICAgCSAgICAgb3V0MS52YWx1ZSA9PSBTRUxGLnZhbHVlICYmIAogICAgIAkgICAgIAkgICAgIG91dDIudmFsdWUgPT0gU0VMRi52YWx1ZSAmJgogICAgIAkgICAgIAkgICAgIGhPdXQxICE9IHdPdXQxICYmIC8vIHJ1bGUgb3V0IHBvaW50IGF0IGluZmluaXR5CiAgICAgCSAgICAgCSAgICAgaE91dDIgIT0gd091dDIgICAgLy8gcnVsZSBvdXQgcG9pbnQgYXQgaW5maW5pdHkgICAgICAgIAoJCSAgICAgICAKICAgICB2YWwgdmFsaWRXID0gcHJvdmVESFR1cGxlKGgsIHcsIGhPdXQxLCB3T3V0MSkgfHwgCiAgICAgCSAgICAgCSAgcHJvdmVESFR1cGxlKGgsIHcsIGhPdXQyLCB3T3V0MikKCiAgICAgLy8gdmFsaWRXIGVuc3VyZXMgdGhhdCBhdCBsZWFzdCBvbmUgb2YgdGhlIG91dHB1dHMgCiAgICAgLy8gaGFzIHRoZSByaWdodCByZWxhdGlvbnNoaXAgYmV0d2VlbiBpdHMgKFI0LCBSNSkKICAgICAgCiAgICAgdmFsaWRXICYmIHZhbGlkT3V0cwogICB9CiAgIAogICBtaXggfHwgb3duZXIKfQ==) is given below:

	{  
	   val h = SELF.R4[GroupElement].get     // current base for dLog
	   val w = SELF.R5[GroupElement].get
	   
	   val owner = proveDHTuple(h, h, w, w)  // = proveDlog(h, w)
	   
	   val mix = {
	     val out1 = OUTPUTS(0)
	     val out2 = OUTPUTS(1)
	     
	     val hOut1 = out1.R4[GroupElement].get
	     val hOut2 = out2.R4[GroupElement].get
	     
	     val wOut1 = out1.R5[GroupElement].get
	     val wOut2 = out2.R5[GroupElement].get
	     
	     val validOuts = out1.propositionBytes == SELF.propositionBytes &&
	     	     	     out2.propositionBytes == SELF.propositionBytes &&
	     	     	     out1.value == SELF.value && 
	     	     	     out2.value == SELF.value &&
	     	     	     hOut1 != wOut1 && // rule out point at infinity
	     	     	     hOut2 != wOut2    // rule out point at infinity        
			       
	     val validW = proveDHTuple(h, w, hOut1, wOut1) || 
	     	     	  proveDHTuple(h, w, hOut2, wOut2)

	     // validW ensures that at least one of the outputs 
	     // has the right relationship between its (R4, R5)
	      
	     validW && validOuts
	   }
	   
	   mix || owner
	}

A mix is done is follows:

1. Pick two boxes with above contract and then generate two mixed boxes as follows:
2. Generate secret bit *b* and use that to decide ordering of outputs
3. Generate secrets *y* for re-randomising inputs. (different secret for each box)
4. Compute *R4^y* and *R5^y* for each input. These are the new R4 and R5 respectively for the outputs. (which output depends on the secret bit *b*)
5. Use secret *y* to generate *proveDHTuple* proofs for both boxes

Any output box is can be remixed simply by picking it up and doing Step 1 to 5 again, without needing the secret protecting the funds.

## Analysis

From Alice's point of view in regard to security, no one should be able to spend the box other than for mixing. Secondly, Alice should always be able to spend her box.  Both are guaranteed by ensuring the exponent relationship between R4, R5 of at least one of the box in the mix, and additionally requiring R4 to be not O (the point at infinity).

With regard to privacy, no outsider should be able to guess with an advantage, which output corresponds to which input. An outsider is anyone other than the inputs' owners and the mixer (which can also be an input's owner).

Let (R4, R5) of the two inputs contain *(h1, w1), (h2, w2)* respectively.  
Then the transformation from input to output for secrets *y1, y1* is:

- For first input: *(h1, w1) -> (h1^y1, w1^y1)*
- For second input: *(h2, w2) -> (h2^y2, w2^y2)*

If the DDH problem is hard then it is hard to distinguish the two outputs.

## Future Work

- The above idea is for two boxes at a time, but this number can be increased to three (and any *fixed*  value) simply by adding the same constraints for the 3rd output along with an additional *proveDHTuple* clause.

- We have the same issue of fee as in ErgoMixer, and a similar solution (using mixing tokens) should work here as well. Other solutions can be considered as well, for instance using 3rd party who takes up the task of mixing for a fee, which can be paid initially.

Finally, note that this is only a high level idea that needs to be further analysed, so please don't experiment with large funds.