Welcome to the loadit wiki!

on this page we will see how to use the Loadit API

**# 1. CREATE OUR PLAYERDATA CLASS**

Since Loadit is in fact a player loader, we need to create a PlayerData class, which implements UserData

![image](https://github.com/Lory9098/loadit/assets/83712762/3fb7a5ba-ca7a-4055-be76-5ac6a4d23035)
This should be the result obtained.

**# 2. CREATE OUR PLAYERDATALOADER CLASS**

To load our data, we need a class (typically called PlayerDataLoader) that implements DataLoader<PlayerData>

``ATTENTION: PlayerData is a test name, it must be replaced with the name of the player class that we created in the first step``

![image](https://github.com/Lory9098/loadit/assets/83712762/1795b238-9eec-4648-a03a-a178729a08cd)

Once we have this class, we insert the code into the methods that have appeared, as in the photo attached above (clearly it changes from code to code), the getOrCreate function is called by loadit when it wants to have a player, consequently, if the player is already loaded in the database (or in the player manager if necessary), you get it from there, otherwise you create it and add it to your database and possibly PlayerManager.

The load method instead means that you have to go and get the player from the database (or PlayerManager)

``ATTENTION: in the methods, "String s" corresponds to the player's name, and "UUID uuid", to his unique identifier.``

**# 3. INITIALIZE LOADIT**

This is the last step. In our Main class we need to create a Loadit<PlayerData> loadit variable;

which will then go into the initialized onEnable. Example:

![image](https://github.com/Lory9098/loadit/assets/83712762/2c95ca59-8ab8-4511-86ad-17409eae9c0e)

**# 4. GETTING PLAYER**

To get the PlayerData object just get the Loadit object (if you followed the part above it's in the main class), and do loadit.getContainer()

and you have access to many methods, including:

Optional<T> getCached(UUID uuid);

T getCached(Player player);

void acceptIfCached(UUID uuid, Consumer<T> consumer);

void acceptIfCached(Player player, Consumer<T> consumer);


And there you have it! Thanks for using Loadit!
