/*
 *  mydb.cpp
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
#include "mydb.h"

using namespace std;

void MyDb::setFileName(string dbname){
	dbFileName = dbname;
	hasError_flag = 0;
}

void MyDb::apprec(Dbt& key, Dbt& data){
 if (!is_open)
 	return;
  mydb.put(NULL,&key, &data,0);
}

Dbc* MyDb::newcursor(){
  if (!is_open)
  	return NULL;
  Dbc* cursorp;
  mydb.cursor(NULL, &cursorp, 0);
  return cursorp;
}

void MyDb::getrec(Dbt& key, Dbt& data){
  if (!is_open)
 	return;
  mydb.get(NULL, &key, &data, 0);
}

void MyDb::delrec(Dbt& key){
  if (!is_open)
 	return;
  mydb.del(NULL,&key,0);
}

void MyDb::dbopen(){
	if (is_open)
		return;
  u_int32_t oFlags = DB_CREATE;
  // oFlags = DB_TRUNCATE;

	try { // Open the database
		mydb.open(NULL, // Transaction pointer 
        dbFileName.c_str(), // Database file name 
        NULL, // Optional logical database name 
        DB_HASH, // Database access method 
        oFlags, // Open flags 
        0); // File mode (using defaults) 
        is_open = true;
        is_closed = false;
	} catch(DbException &e) { 
		cout << "Opening DB for create failed for db exception: \n  " << e.what() << endl;
	} catch(std::exception &e) { 
		cout << "Opening DB for create failed for other exception" << endl;
	}
}
	

void MyDb::hashopen(){
	if (is_open)
		return;
	
	u_int32_t oFlags = DB_CREATE;
	// oFlags = DB_TRUNCATE;
	
	try { // Open the database
		mydb.open(NULL, // Transaction pointer 
				  dbFileName.c_str(), // Database file name 
				  NULL, // Optional logical database name 
				  DB_HASH, // Database access method 
				  oFlags, // Open flags 
				  0); // File mode (using defaults) 
		is_open = true;
		is_closed = false;
	} catch(DbException &e) { 
		cout << "Opening DB for create failed for db exception" << endl;
	} catch(std::exception &e) { 
		cout << "Opening DB for create failed for other exception" << endl;
	}
}

void MyDb::dbread(){
	if (is_open)
		return;
	u_int32_t oFlags = DB_RDONLY;  
	//u_int32_t oFlags = DB_TRUNCATE;                                             
	try { // Open the database                                                    
			mydb.open(NULL, // Transaction pointer                
			dbFileName.c_str(), // Database file name                           
            NULL, // Optional logical database name                            
          DB_HASH, // Database access method 
          oFlags, // Open flags                                       
          0); // File mode (using defaults)    
          is_closed = false;
          is_open = true; 
	} catch(DbException &e) {
		cout << "Opening DB for read-only failed for db exception" << endl;
	} catch(std::exception &e) {
		cout << "Opening DB for read-ony failed for other exception" << endl;
	}
}

void MyDb::hashread(){
	if (is_open)
		return;
	u_int32_t oFlags = DB_RDONLY;  
	//u_int32_t oFlags = DB_TRUNCATE;                                             
	try { // Open the database                                                    
		mydb.open(NULL, // Transaction pointer                
				  dbFileName.c_str(), // Database file name                           
				  NULL, // Optional logical database name                            
				  DB_HASH, // Database access method 
				  oFlags, // Open flags                                       
				  0); // File mode (using defaults)     
		is_closed = false;
          is_open = true; 
	} catch(DbException &e) {
		cout << "Opening DB for read-only failed for db exception" << endl;
	} catch(std::exception &e) {
		cout << "Opening DB for read-only failed for other exception" << endl;
	}
}


int MyDb::close() {
	if (is_closed)
		return 0;
	try { // Close the database 
		mydb.close(0); 
		is_closed = true;
		is_open = false;
	} catch(DbException &e) { 
		lastErrorMessage = "Closing DB failed for db exception:\n ";
		lastErrorMessage += e.what();
		hasError_flag = 1;
		return -1;
	} catch(std::exception &e) { 
		cout << "Closing DB failed for other exception" << endl;
		return -1;
	}
	return 0;
}

string MyDb::getLastErrorMessage() {
	hasError_flag = 0;
	return lastErrorMessage;
}

int MyDb::hasError() {
	return hasError_flag;
}