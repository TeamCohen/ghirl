/*
 *  mydb.h
 *  
 *
 *  Created by Cathrin Weiss   
 *  http://www.ifi.uzh.ch/ddis/people/weiss/
 *	mailto:weiss@ifi.uzh.ch
 *  (c) University of Zurich, 2009
 //---------------------------------------------------------------------------
 // This work is licensed under the Creative Commons
 // Attribution-Noncommercial-Share Alike 3.0 Unported License. To view a copy
 // of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/
 // or send a letter to Creative Commons, 171 Second Street, Suite 300,
 // San Francisco, California, 94105, USA.
 //---------------------------------------------------------------------------
 *
 */

#include <db_cxx.h> 
#include <string>

using namespace std;

#ifndef MYDB_H
#define MYDB_H

class MyDb { 
public: // Constructor requires a path to the database database name. 
 MyDb() : mydb(NULL, 0) {}; 
 MyDb(string dbname): mydb(NULL,0), dbFileName(dbname){ dbopen(); };

// Our destructor just calls our private close method. 
  ~MyDb() { close(); }; 
	void setFileName(string dbname);
	void dbopen();
	void dbread();
	void hashopen();
	void hashread();
	
	void apprec(Dbt& key, Dbt& Data);
	Dbc* newcursor();
	void getrec(Dbt& key, Dbt& Data);
	void delrec(Dbt& key);

	inline Db &getDb() {return mydb;} 

private: 
	Db mydb; 
	string dbFileName; 
	u_int32_t cFlags; 

	void close(); 
};

#endif
