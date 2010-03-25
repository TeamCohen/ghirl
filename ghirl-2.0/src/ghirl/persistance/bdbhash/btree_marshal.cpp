/*
 *  btree_marshal.cpp
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

#include <string>
#include <set>
#include <map>
#include "BDB/mydb.h"
using namespace std;

enum nodetype {
	NODES, EDGES, PROPS
};



struct mapping {
	int mid;
	std::string mvalue;
	
	char *make_id_key_buffer(){
		char *buffer = new char[sizeof(int)];
		int bufLen = 0;
		
		memcpy(buffer+bufLen, &mid, sizeof(int));
		bufLen += sizeof(int);
	
		
		return buffer;
	}
	
	char *make_val_key_buffer(){
		char *buffer = new char[mvalue.size() + 1];
		int bufLen = 0;
		const char *cmvalue = mvalue.c_str();
		memcpy(buffer+bufLen, cmvalue, mvalue.size() + 1);
		bufLen += mvalue.size() + 1;
		
		return buffer;
	}
	
	void val_from_buffer(void *buffer){
		char *buf = (char*)buffer;
		mvalue = std::string(buf);
	}
	
	void id_from_buffer(void *buffer){
		char *buf = (char*)buffer;
		mid = *((int*)buf);
	}
	
};

struct btree_data {
	int id_first;
	int id_second;
	nodetype type;
	int *tail; 
	int tail_size;
	
	btree_data(int bid1, int bid2, nodetype btype){
		this->id_first = bid1;
		this->id_second = bid2;
		this->type = btype;
	}
	
	void make_tail(set<int> tset){
		this->tail_size = tset.size();
		this->tail = new int[this->tail_size];
		set<int>::iterator it = tset.begin();
		int i = 0;
		while (it != tset.end()){
			tail[i++] = *it;
			it++;
		}
	}
	
	char *make_key_buffer(){
		char *buffer = new char[(2*sizeof(int)+sizeof(nodetype))];
		int bufLen = 0;
		
		memcpy(buffer+bufLen, &id_first, sizeof(int));
		bufLen += sizeof(int);
		
		memcpy(buffer+bufLen, &id_second, sizeof(int));
		bufLen += sizeof(int);
		
		memcpy(buffer+bufLen, &type, sizeof(nodetype));
		bufLen += sizeof(nodetype);
		
		return buffer;
	}
	
	int get_key_buffer_size(){
		return (2*sizeof(int)+sizeof(nodetype));
	}
	
	int get_data_buffer_size(){
		return (( (tail_size) * sizeof(int) )+ sizeof(int));
	}
	
	char *make_data_buffer(){
		char *buffer = new char[get_data_buffer_size()];
		int bufLen = 0;
		
		memcpy(buffer+bufLen, &tail_size, sizeof(int));
		bufLen += sizeof(int);
		
		memcpy(buffer+bufLen, tail, tail_size*sizeof(int));
		bufLen += tail_size * sizeof(int);
		
		delete []this->tail;
		return buffer;
	}
	
	void tail_from_buffer(void *buf){
		int bufLen = 0;
		char *buffer = (char*)buf;
		
		this->tail_size = *((int*)buffer);
		bufLen += sizeof(int);
		
		// only delete if *buf wasn't constant... 
		this->tail = (int*)(buffer+bufLen); 
		bufLen += this->tail_size * sizeof(int);
	}
	
};
