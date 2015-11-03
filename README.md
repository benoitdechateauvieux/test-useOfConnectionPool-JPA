Simple unit test that shows that JPA picks a connection from the connection pool as soon as a transaction is started.   

Conclusions:
* Creating an Entity Manager doesn't open a connection
* Starting a transaction opens immediately a connection
* Committing/Rollbacking a transaction doesn't release the connection
* Closing an Entity Manager release immediately the connection (if there is no active transaction)
* Once a connection is associated with an Entity Manager, it is released only by a call to em.close()