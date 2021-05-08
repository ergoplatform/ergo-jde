# JSON dApp Environment (JDE)

JDE is a tool for interfacing with dApps on the Ergo blockchain. 

### What is it?
JDE is similar to [Ergo-AppKit](https://github.com/ergoplatform/ergo-appkit) and the [Headless dApp Framework (HDF)](https://github.com/Emurgo/ergo-headless-dapp-framework) in that all three are tools to interact with the Ergo blockchain. 
Developers can use these tools to read data from the blockchain, compute using that data and optionally create transactions to be
broadcast. Each tool requires the developer to "program" in some language. 

Users of AppKit will usually write Scala code (although AppKit supports many other languages). HDF users will need to write Rust code. 
JDE users will have to write JSON, which has arguably the easiest to learn of the three. The downside of using JSON is that JDE scripts are more
verbose than the other two languages. For example, the JDE equivalent of Scala's `a = b + c` is `{"name":"a", "left":"b", "op":"add", "right":"c"}`.

JDE is not intended for non-tech users. 

### Capabilities

JDE can be used for any of the following tasks: 

1. Reading data from the blockchain. As an example we could use JDE to get the following data:
   
   (a) The current ERG/USD rate determined by the ERG/USD oracle.
   
   (b) The Sigma-USD bank's Erg balance and stable-coins in circulation.   
 
   The data must be stored in an unspent box (JDE cannot access spent boxes).
    
2. Computing using the above data and returning the result. For example: 
  
   (a) The current reserve ratio of the Sigma-USD bank.

   (b) The current rate of Sigma-USD reserve and stable coins.

3. Create a *Transaction Template* for a transaction using the above values such as one for minting some Sigma-USD reserve coins.
   

#### Transaction Template   
A transaction template is like an unsigned transaction with the difference that while an unsigned transaction is *complete* in the sense that it contains *all* the inputs, outputs and data-inputs of the final transaction, a template contains only a subset of this data. In particular, it contains:
   
   1. The boxIds of inputs to be used.
       
   2. The boxIds of data-inputs to be used.
       
   3. Outputs to be created (encoded in the same way that the Ergo node understands).

   4. The total tokens and Ergs contained in the inputs for the selected boxIds.

   5. The total tokens and Ergs needed for the outputs that are created.
   
JDE does not ensure that the input and output Ergs/tokens are conserved. In fact, most of the time the input Ergs could be falling short,
and it is up to the user (or the wallet software) to make up and make the transaction complete by adding more inputs 
(and possibly a change output if the wallet does not automatically add one). An example of using this information to create a
complete transaction is in the [KioskWeb wallet](https://github.com/scalahub/KioskWeb/blob/main/src/main/scala/kiosk/wallet/KioskWallet.scala#L124).

### Running JDE

The following instructions are for Linux and MacOS. For Windows, please adapt the instructions accordingly.

- Clone the project using the command `git clone https://github.com/ergoplatform/ergo-jde.git`.
- Ensure that you have SBT installed.

Currently, there are two ways to run JDE. 

#### CLI

- Create a fat jar using the command `sbt assembly`. 
- A jar called `jde.jar` will be created in the folder `target/scala-2.12`.

To run the CLI issue the command `java -jar jde.jar`. It will print instructions to invoke JDE:

    > java -jar jde.jar 
    
    Usage java -jar <jarFile> <script.json>

The second parameter contains the file with the JSON script. The folder `/sample-scripts` contains some sample JDE programs.  
As an example the following command will return information about the SigmaUSD stablecoin (such as the current rate). 

    java -jar jde.jar getStableCoinInfo.json

The above command should return the same data as in the web-service example below (ensure that both the jar and json files are in the current directory).

#### Web Service

JDE web-service can be run in two ways. 

1. Using the embedded web-server (Jetty): This is the easiest way and should be used for development mode. 
   
   - Open SBT console within the root project folder using the command `sbt`.
   - In the SBT shell issue the command `jetty:start`.
   - The service will start listening on http://localhost:8080
    
2. Using an external J2EE web-server.
   - Generate the war file using the command `sbt package`. The war file will be created in `target/scala-2.12` folder. 
   - This file can be run under any standard J2EE web server. The service will start listening on the port configured in the web-server

In both cases the endpoint `/compile` will be exposed for sending requests.

The following is the output of the command `curl --data @sample-scripts/getStableCoinInfo.json http://localhost:8080/compile`

```
{
  ...
  "returned" : [ {
    ...
    "name" : "reserveRatioPercentIn",
    "value" : "403"
    ...
    "name" : "rate",
    "value" : "2506549"
  } ]
}
```
The above snippet shows the current reserve ratio of the bank along with the current stablecoin rate in nanoErgs. 

### Programming JDE

A user interacts with JDE using a structure called a *Program*, a JSON-encoded script defining the following primary components:

1. Inputs: These contain instructions to search for boxes that will form the inputs of the transaction.
2. Data-Inputs: These contain instructions to search for boxes that will form the data-inputs of the transaction.
3. Outputs: These contain instructions to construct boxes that will form the outputs of the transaction. 

A JDE program can also have other components such as:
1. Auxiliary inputs: These contain instructions to search for boxes whose data we need to use in computation but such boxes need not be included in the transaction.
As an example, the voting protocol of Sigma-USD requires R5 of the ballot boxes to contain the boxId of the update box. However, the update box itself is not used
   in the voting transaction. In this case, the update box will be specified as an auxiliary input. 

2. Post-conditions: These are used to ensure that certain conditions are satisfied after the final step. As an example,
for Sigma-USD, we can have post-conditions to check that the reserve ratio is between the specified values. 
   
3. Branches: These can be used to handle if-then-else cases. Note that JDE does not support loops. 

4. Constants: These can be used to define constants (such as the Sigma-USD bank address or NFT id) for later use.

5. Returned values: These can contain values that the end-user wants, such as the current Sigma-USD stable-coin rate.

6. Binary and unary operations: These define operations to apply on any of the values obtained earlier. 

Each of the above components are optional. If the outputs are not specified then the transaction template won't be created.

The folder `/sample-scripts` contains several sample JDE programs. 

See [this document](/syntax.md) for the syntax of JDE programs.