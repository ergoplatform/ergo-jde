# JSON dApp Environment (JDE)

JDE can be thought of as a "Programmable" transaction builder for the Ergo blockchain, with JSON being the programming language.
JDE is primarily intended for developers.

### What can it do?

Recall that in an Ergo dApp, building a transaction usually involves a combination of the following tasks:
1. Get some unspent boxes from the blockchain with some specific properties (such as with a given address and/or containing a given NFT)
2. Extract data from those boxes (tokens, registers) and compute some values using the data.
3. Define output boxes from the above data and create a transaction request to get an unsigned transaction.
4. Use a wallet software (such as the Ergo node) to sign the transaction.
 
JDE is designed for Steps 1-3. There is also a connector for Ergo node to perform Step 4.

### What can it be compared to? 

JDE is similar to [Ergo-AppKit](https://github.com/ergoplatform/ergo-appkit) and the [Headless dApp Framework (HDF)](https://github.com/Emurgo/ergo-headless-dapp-framework) in that all three are tools to interact with the Ergo blockchain. 
Developers can use these tools to read data from the blockchain, compute using that data and optionally create transactions to be
broadcast. Each tool requires the developer to "program" in some language. 

Users of AppKit will usually write Scala code (although AppKit supports many other languages). HDF users will need to write Rust code. 
JDE users will have to write JSON, which is arguably the easiest to learn of the three. The downside of using JSON is that JDE scripts are more
verbose than the other two languages. For example, the JDE equivalent of Scala's `a = b + c` is `{"name":"a", "left":"b", "op":"add", "right":"c"}`.

The folder [sample-scripts](/sample-scripts) contains some sample JDE programs.

### Core Capabilities

The following are the core capabilities of JDE: 

1. Reading data from the blockchain. As an example we could use JDE to get the following data:
   
   (a) The current ERG/USD rate determined by the ERG/USD oracle.
   
   (b) The Sigma-USD bank's Erg balance and stable-coins in circulation.   
 
   The data must be stored in an unspent box (JDE cannot access spent boxes).
    
2. Computing using the above data and returning the result. For example: 
  
   (a) The current reserve ratio of the Sigma-USD bank.

   (b) The current rate of Sigma-USD reserve and stable coins.

3. Create a *Transaction Template* for a transaction using the above values such as one for minting some Sigma-USD reserve coins.
   

### Transaction Template

An Ergo **unsigned transaction** (UT) is a tuple **(inputs, data-inputs, outputs)** such that the following hold:
- The sum of nano-ergs is the inputs is exactly the same as the sum of the nano-ergs in the outputs.
- The sum of tokens in the inputs is same or greater than the sum of tokens in the outputs. 
- There is a fee output box

For brevity, we represent boxes using boxIds. 
Thus, a UT is a tuple **(input-boxIds, data-input-boxIds, outputs)** such that the above conditions hold.

A **transaction template** (TT) is similar to an unsigned transaction in that it is also a tuple **(input-boxIds, data-input-boxIds, outputs)**. 
However, none of the above conditions are required to hold in a TT. 

For a given script, JDE outputs a TT along with some metadata such as the sum of nano-ergs and tokens in the inputs and outputs.
In particular, JDE does not ensure that the input and output nano-ergs/tokens are conserved.
The metadata can be used by the wallet software to create a UT from the TT by adding a fee output and optional funding and change outputs.
An example of this is in the [KioskWeb wallet](https://github.com/scalahub/KioskWeb/blob/main/src/main/scala/kiosk/wallet/KioskWallet.scala#L124).

On the other hand, we can also write JDE scripts where the output TT is also a UT (by specifying funding, change and fee outputs in the script itself). 
In this case, the TT can be directly used for generating the final signed transaction. 
An example of such a script is the [advanced mint reserve-coin script](/sample-scripts/mintReserveCoinAdvanced.json).   

### Running JDE

Depending on the use-case and the actual script, JDE has the following entry-points (modes).
- **Compile**: Used to run JDE in "vanilla mode", i.e., return a TT. This is also the default mode in CLI and web-service (see below),
- **Request**: Runs Compile mode and uses its output to create a transaction creation request. This is useful only if the TT is also a UT. This also needs access to a fully synced Ergo node.
- **Generate**: Runs Request mode and uses its output to generate a signed transaction. This is useful only if the TT is also a UT. This also needs access to a fully synced Ergo node and its api key.
- **Send**: Runs Generate mode and uses its output to send a signed transaction. This is useful only if the TT is also a UT. This also needs access to a fully synced Ergo node and its api key.

The following instructions are for Linux and MacOS. For Windows, please adapt the instructions accordingly.

- Clone the project using the command `git clone https://github.com/ergoplatform/ergo-jde.git`.
- Ensure that you have SBT installed.

Currently, there are two ways to run JDE: via a CLI and as a web-service. 

### Using CLI

In order to use the CLI, first create a fat jar using the command `sbt assembly`. A jar called `jde.jar` will be created in the folder `target/scala-2.12`.

The CLI supports all 4 modes of operation, with the default being "Compile".

- **Compile**: To run the CLI in this mode (the default) use the command `java -jar jde.jar`. It will print instructions:

      Usage: java -jar jde.jar <script.json>
    
   The first parameter is the file with the JSON script. 
  
   As example usage of this command is: 
      
      java -jar jde.jar getStableCoinInfo.json 

   Notes: 
    - Ensure that both the jar and json files are in the current directory)
    - The file `getStableCoinInfo.json` is present in the [sample-scripts](/sample-scripts) folder.
    - The script is designed to return information about the SigmaUSD stable-coin (such as the current rate).
   
   The above command should return the same data as that returned in the web-service example below.


- **Request**: To run the CLI in this mode use the command `java -cp jde.jar cli.Request`. It will print instructions:

      Usage: java -cp jde.jar cli.Request <script.json> <ergo-node-url>

   - The first parameter is the file with the JSON script.
   - The second parameter is the full base-url of the node.
   - This requires the TT output by the script to be a UT, otherwise an error is thrown.
   
   An example usage of this command is:

      java -cp jde.jar cli.Request mintReserveCoinAdvanced.json http://192.168.0.200:9053

   The above command will return a transaction request to be sent to the node.


- **Generate**: To run the CLI in this mode use the command `java -cp jde.jar cli.Generate`. It will print instructions:

      Usage: java -cp jde.jar cli.Generate <script.json> <ergo-node-url> <api-key>

   - The first parameter is the file with the JSON script.  
   - The second parameter is the full base-url of the node.
   - The third parameter is the api-key of the node.
   - This requires the TT output by the script to be a UT, otherwise an error is thrown.
    
   An example usage of this command is:
 
      java -cp jde.jar cli.Generate mintReserveCoinAdvanced.json http://192.168.0.200:9053 hello

   The above command will return a signed transaction ready to be broadcast.


- **Send**: To run the CLI in this mode use the command `java -cp jde.jar cli.Send`. It will print instructions:

      Usage: java -cp jde.jar cli.Send <script.json> <ergo-node-url> <api-key>

   - The first parameter is the file with the JSON script.
   - The second parameter is the full base-url of the node.
   - The third parameter is the api-key of the node.
   - This requires the TT output by the script to be a UT, otherwise an error is thrown.
   
   An example usage of this command is:

      java -cp jde.jar cli.Send mintReserveCoinAdvanced.json http://192.168.0.200:9053 hello

   The above command will broadcast the transaction and print the transaction id.

### Using the web service

Unlike the CLI, the web service currently only supports the **Compile** mode. 
The web-service can be run in two ways. 

1. Using the embedded web-server (Jetty): This is the easiest way and should be used for development mode. 
   
   - Open SBT console within the root project folder using the command `sbt`.
   - In the SBT shell issue the command `jetty:start`.
   - The service will start listening on http://localhost:8080
    
2. Using an external J2EE web-server.
   - Generate the war file using the command `sbt package`. The war file will be created in `target/scala-2.12` folder. 
   - This file can be run under any standard J2EE web server. The service will start listening on the port configured in the web-server

In both cases the endpoint `/compile` will be exposed for sending requests. The endpoint accepts both GET and POST requests 
where the body contains the script code. The following shows how to call the POST endpoint using cURL. 

    curl --data @sample-scripts/getStableCoinInfo.json http://localhost:8080/compile

The following is a sample response from the above call:

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
The above returns the current reserve ratio of the SigmaUSD bank along with the current stable-coin rate. 

### Writing JDE Scripts

A user interacts with JDE using a structure called a *Program* ([source](/jde/src/main/scala/jde/compiler/model/External.scala#L17)), a JSON-encoded script defining the following primary components:

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

Unspent boxes can be searched by address and/or box-id. With box-id, there can be at most one box, so we can unambiguously define a box.
  However, when matching with address, there can be multiple boxes. These are handled as follows:
   - The selected boxes are filtered using registers, tokens and nano-ergs.
   - The resulting boxes are then sorted by nano-erg value in decreasing order.
   - The first box (if any) is selected as the matched box. If the `multi` flag is set then all boxes are selected.
- An error is thrown if no boxes match a definition, unless the `optional` flag is set.

The folder [sample-scripts](/sample-scripts) contains several sample JDE programs. The [tests](/jde/src/test/scala/jde) contain more examples. 

See [this document](/syntax.md) for the detailed semantics of JDE programs.
