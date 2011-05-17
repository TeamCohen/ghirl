/*
 *  hexastore.cc
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


#include "hexastore.h"
#include <sstream>

#define DEBUG 0


ObjectMapTwoL edges;
ObjectMapTwoL props;
ObjectMapOneL nodes;  // / key : -1 -> allNodes , key : -2 -> number of nodes 

map<string, int> s_id;
map<int, string> id_s;

int iid = 0;



string dict_db = "NULL";
string data_db = "NULL";

MyDb *mapDB; 
MyDb *mymdb;
int flag = 0;



//===========================================


int is_anumber(char *s){
	int slen = strlen(s)-1;
	int tens = 1;
	int result = 0;
	for (int i = slen; i >= 0; i--){
		if (48 > s[i])
			return 0;
		if (57 < s[i])
			return 0;
		
		result += (tens * s[i]);
		tens *= 10;
	}
	if (DEBUG == 2) {
		cout << "is number: " << s << " ... " << result << endl;
	}
	return result;
}

//===========================================

void read_in_mapping(){
	Dbc *cursorp;
	
	try {
		// Database open omitted for clarity
		
		// Get a cursor
		cursorp = mapDB->newcursor(); 
		
		Dbt key, data;
		int ret;
		
		// Iterate over the database, retrieving each record in turn.
		while ((ret = cursorp->get(&key, &data, DB_NEXT)) == 0) {
			char *kdata = (char*)key.get_data();
			if (strlen(kdata) <= sizeof(int)) {
				if (is_anumber(kdata)){
					int num = *((int*)kdata);
					
					kdata = (char*)data.get_data();
					string s = string(kdata);
					id_s[num] = s;
					s_id[s] = num;
				}
			}
			
		}
		if (ret != DB_NOTFOUND) {
			// ret should be DB_NOTFOUND upon exiting the loop.
			// Dbc::get() will by default throw an exception if any
			// significant errors occur, so by default this if block
			// can never be reached. 
		}
	}  catch(std::exception &e) {
        printf("Error! %s\n", e.what());
	}
	
	// Cursors must be closed
	if (cursorp != NULL) 
		cursorp->close(); 
	
}

void open_DB(const char* mode = "w"){

	if (strcmp(dict_db.c_str(), "NULL") == 0 || strcmp(data_db.c_str(), "NULL") == 0) {
		cout << "[DB ERROR] You need to set a database base name first by using setBase(..). DB cannot be opened" << endl;
		return;
	}
	if (mapDB == NULL && mymdb == NULL) {
		mapDB = new MyDb(dict_db);
		mymdb = new MyDb(data_db);
		if (strcmp(mode, "r") == 0){
			mapDB->dbread();
			mymdb->dbread();
		} else {
			mapDB->dbopen();
			mymdb->dbopen();
		}
		if (flag == 0) {
			read_in_mapping();
			flag = 1;
		}	
	}
	
}



string int_to_string(int i){
	
	std::string s;
	std::stringstream out;
	out << i;
	s = out.str();
	return s;
}


void write_to_db(MyDb &mdb,  int id1, int id2, ObjectSet &s, nodetype t){
	
	btree_data bdd(id1, id2, t);
	bdd.make_tail(s);
	char *bkbuf = bdd.make_key_buffer();
	char *bdbuf = bdd.make_data_buffer();
	Dbt key((void*)bkbuf , bdd.get_key_buffer_size());
	Dbt data((void*)bdbuf, bdd.get_data_buffer_size());
	
	mdb.apprec(key,data);
	delete []bkbuf;
	delete []bdbuf;
}



set<int> write_to_db(MyDb &mdb, int id, ObjectMapOneL &mp, nodetype t, ObjectSet &s){
	ObjectMapOneL::iterator it = mp.begin();
	
	while (it != mp.end()){
		write_to_db(mdb, id, it->first, it->second, t);
		s.insert(it->first);
		++it;
	}
	return s;
}

void write_to_db(MyDb &mdb, int id, ObjectMapOneL &mp, nodetype t){
	map<int, set <int> >::iterator it = mp.begin();
	while (it != mp.end()){
		write_to_db(mdb, id, it->first, it->second, t);
		++it;
	}
}

void write_to_db(MyDb &mdb, ObjectMapTwoL &mp, nodetype t1, nodetype t2){
	ObjectMapTwoL::iterator it = mp.begin();
	
	
	while (it != mp.end()) {
		set<int> middle;
		middle= write_to_db(mdb, it->first, it->second, t2, middle);
		write_to_db(mdb,  0, it->first, middle, t1);
		++it;
	}
	
}

void write_nodes_to_db(MyDb &mdb){
	int *allnodes = new int[nodes.size()];
	mapping m;
	m.mid = -2;

	Dbt key((void*)(m.make_id_key_buffer()), sizeof(int));
	m.mid = nodes.size();
	Dbt data((void*)(m.make_id_key_buffer()), sizeof(int));
	mdb.apprec(key,data);
	

	int i = 0;
	ObjectMapOneL::iterator it = nodes.begin();
	while (it != nodes.end()){
		allnodes[i++] = it->first;
		write_to_db(mdb,  0, it->first, it->second, NODES);
		++it;
	}
	
	btree_data bdd(0, -1, NODES);
	bdd.tail = allnodes;
	bdd.tail_size = nodes.size();
	m.mid = -1;
	char *bdbuf = bdd.make_data_buffer();
	Dbt key2((void*)(m.make_id_key_buffer()) , sizeof(int));
	Dbt data2((void*)bdbuf, bdd.get_data_buffer_size());
	
	mdb.apprec(key2,data2);
	
	delete []bdbuf;
	
}


void write_edges_props_to_db(MyDb &mdb){
	write_to_db(mdb, edges, EDGES, EDGES);
	cout << "edges written" << endl;
	write_to_db(mdb, props, PROPS, PROPS);
	cout << "props written" << endl;
	
}

void write_all_db(MyDb &mdb){
	cout << "write nodes" << endl;
	write_nodes_to_db(mdb);
	cout << "nodes written" << endl;
	write_edges_props_to_db(mdb);
	
}


void test_read(MyDb &mdb, int id1, int id2, nodetype t){
	Dbt data;
	btree_data bdd(id1, id2, t);
	Dbt key((void*)(bdd.make_key_buffer()), bdd.get_key_buffer_size());
	
	mdb.getrec(key, data);
	
	char *buf = (char*)data.get_data();
	
	if (buf != NULL){
		int size = *((int*)buf);
		cout << size << endl;
		
		int *l;
		l = (int*)(buf+sizeof(int));
		cout << l[0]  <<  endl;
	} else {
		cout << "no entries for this combination" << endl;
	}
}

string s_mapping(MyDb &mdb, int sid){
	
	if (id_s.count(sid) == 0){
	
		mapping *m = new mapping();
		m->mid = sid;
		Dbt mkey((void*)(m->make_id_key_buffer()), sizeof(int));
		Dbt mdata;
		mdb.getrec(mkey, mdata);
		if (mdata.get_data() != NULL){
			char *sdata = (char*)(mdata.get_data());
			m->mvalue = string(sdata);
			id_s[sid] = m->mvalue;
			delete m;
		} else {  //  should never happen ... 
			delete m;
			return "";
		}
		
	} 
	
	if (DEBUG == 1){
		cout << "[S MAPPING] " << sid << " : " << id_s[sid] << endl; 
		
	}
	return id_s[sid];
	
}

int read_mapping(MyDb &mdb, string value){
	
	if (s_id.count(value) > 0){
		return s_id[value];
	}
	mapping *m = new mapping();
	
	m->mvalue = value;
	mapping maxmap;
	maxmap.mid = -1;
	Dbt mkey((void*)(maxmap.make_id_key_buffer()), sizeof(int));
	Dbt mdata;
	
	if (iid == 0) {
		
		mdb.getrec(mkey, mdata);
		char *buff = (char*)mdata.get_data();
		if (buff != NULL){
			iid = *((int*)buff);
		}	
	}
	Dbt key((void*)(m->make_val_key_buffer()), (value.size()+1)*sizeof(char));
	Dbt data;
	mdb.getrec(key, data);
	int retval;
	char *buf = (char*)data.get_data();
	if (buf == NULL){
		m->mid = ++iid;
		Dbt data2((void*)m->make_id_key_buffer(), sizeof(int));
		mdb.apprec(key, data2);
		mdb.apprec(data2, key);
		
		char *intbuf = new char[sizeof(int)];
		memcpy(intbuf, &(m->mid), sizeof(int));
		Dbt mdata2((void*)intbuf, sizeof(int));
		mdb.apprec(mkey, mdata2);
		delete []intbuf;
		retval = m->mid;
	} else {
	
		retval = *((int*)buf);
		
		
	}
	
	
	delete m;
	s_id[value] = retval;
	id_s[retval] = value;
	
	if (DEBUG == 1){
		cout << "[MAPPING] " << value << " : " << retval << endl; 
	
	}
	return retval;
}

//===========================================

int is_number(const char *s){
	int slen = strlen(s)-1;
	int tens = 1;
	int result = 0;
	for (int i = slen; i >= 0; i--){
		if (48 > s[i])
			return 0;
		if (57 < s[i])
			return 0;
		
		result += (tens * s[i]);
		tens *= 10;
	}
	if (DEBUG == 2) {
		cout << "is number: " << s << " ... " << result << endl;
	}
	return result;
}




int put_edge(const char *sc, const char *pc, const char *oc){
	string s(sc);
	string p(pc);
	string o(oc);
	if (s_id.count(s) == 0){
		read_mapping(*mapDB, s);
	}
	if (s_id.count(p) == 0){
		read_mapping(*mapDB, p);
	}
	if (s_id.count(o) == 0){
		read_mapping(*mapDB, o);
	}
	edges[s_id[s]][s_id[p]].insert(s_id[o]);
	
	return edges.size();
		
}

btree_data *read_edges(MyDb &mdb, int id1, int id2){
	btree_data *bdd = new btree_data(id1, id2, EDGES);
	char *bkbuf = bdd->make_key_buffer();
	Dbt key((void*)bkbuf , bdd->get_key_buffer_size());
	Dbt data;
	bdd->tail_size = -1;
	mdb.getrec(key, data);
	if (data.get_data() != NULL){
		bdd->tail_from_buffer(data.get_data());
	}
	
	delete []bkbuf;
	return bdd;

}


btree_data *read_props(MyDb &mdb, int id1, int id2){
	btree_data *bdd = new btree_data(id1, id2, PROPS);
	bdd->tail_size = -1;
	char *bkbuf = bdd->make_key_buffer();
	Dbt key((void*)bkbuf , bdd->get_key_buffer_size());
	Dbt data;
	
	mdb.getrec(key, data);
	if (data.get_data() != NULL){
		bdd->tail_from_buffer(data.get_data());
	}
	
	delete []bkbuf;
	return bdd;
	
}

btree_data *read_nodes(MyDb &mdb){
	mdb.dbread();
	btree_data *bdd = new btree_data(0, -1, NODES);
	bdd->tail_size = -1;
	mapping m;
	m.mid = -1;
	char *bkbuf = m.make_id_key_buffer();
	Dbt key((void*)bkbuf, sizeof(int));
	Dbt data;
	
	mdb.getrec(key, data);
	if (data.get_data() != NULL){
		bdd->tail_from_buffer(data.get_data());
	} 
	
	delete []bkbuf;
	return bdd;
	
}

btree_data *get_from(const char *s, const char *p) {
	int sid, pid;
	string pstring = string(p);
	sid = 0;
	if (strcmp(s, "") != 0){
	
		string sstring = string(s);
	
		if (s_id.count(sstring) == 0){
			sid = read_mapping(*mapDB, sstring);
		} else {
			sid = s_id[sstring];
		}
	}
	if (s_id.count(pstring) == 0){
		pid = read_mapping(*mapDB, pstring);
	} else {
		pid = s_id[pstring];
	}
	
	return read_edges(*mymdb, sid, pid);
}


int put_prop(const char *sc, const char *pc, const char *oc){
	string s(sc);
	string p(pc);
	string o(oc);
	if (s_id.count(s) == 0){
		read_mapping(*mapDB, s);
	}
	if (s_id.count(p) == 0){
		read_mapping(*mapDB, p);
	}
	if (s_id.count(o) == 0){
		read_mapping(*mapDB, o);
	}
	
	props[s_id[s]][s_id[p]].insert(s_id[o]);

	return props.size();
	
}

string get_prop(const char *so, const char *po) {	
	string s(so);
	string p(po);
	if (s_id.count(s) == 0){
		read_mapping(*mapDB, s);
	}
	if (s_id.count(p) == 0){
		read_mapping(*mapDB, p);
	}
	
	if (props.count(s_id[string(s)]) > 0)
		if (props[s_id[string(s)]].count(s_id[string(p)]) > 0)
			return id_s[*(props[s_id[string(s)]][s_id[string(p)]].begin())];
		else {
			btree_data *bd = read_props(*mymdb, s_id[s], s_id[p]);
			if (bd->tail_size < 0) {
				delete bd;
				return "";
			}
			string prop = s_mapping(*mapDB, bd->tail[0]);
			return prop;
		}
	return "";
}

int contains_node(const char *sc){
	
	if (DEBUG == 1){
		cout << "[CONTAINS NODE] " << sc << endl; 	
	}
	
	
	string s(sc);
	
	if (s_id.count(s) == 0){
		read_mapping(*mapDB, s);
	}
	
	if (nodes.count(s_id[s]) == 0 ){
		btree_data *bdd = new btree_data(0, s_id[s], NODES);
		char *bkbuf = bdd->make_key_buffer();
		Dbt key((void*)bkbuf , bdd->get_key_buffer_size());
		Dbt data;
	
		mymdb->getrec(key, data);
		if (data.get_data() != NULL){
			bdd->tail_from_buffer(data.get_data());
			for (int j = 0 ; j < bdd->tail_size ; j++){
				nodes[s_id[s]].insert(bdd->tail[j]);
			}
			
			delete []bkbuf;
			delete bdd;
			if (DEBUG == 1){
				cout << "[CONTAINS NODE] TRUE"<< endl; 
				
			}
			return 1;
		}
		delete []bkbuf;
		delete bdd;
		if (DEBUG == 1){
			cout << "[CONTAINS NODE] FALSE" << endl; 
			
		}
		return 0;
	}
	if (DEBUG == 1){
		cout << "[CONTAINS NODE] TRUE"<< endl; 
		
	}
	return 1;
}



int make_node(const char *sc, const char *pc){
	string s(sc);
	string p(pc);
	
	if (s_id.count(s) == 0){
		read_mapping(*mapDB, s);
	}
	if (s_id.count(p) == 0){
		read_mapping(*mapDB, p);
	}
	
	nodes[s_id[s]].insert(s_id[p]);

	return nodes.size();
}

//=============================================================



JNIEXPORT jint JNICALL Java_ghirl_persistance_Hexastore_addAnEdge(JNIEnv *env, jobject obj, jstring arg1, jstring arg2, jstring arg3) {
	/* Convert to UTF8 */
	open_DB();
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	const char *p  = env->GetStringUTFChars(arg2, JNI_FALSE);
	const char *o  = env->GetStringUTFChars(arg3, JNI_FALSE);

	/* Call into external dylib function */
	jint rc = put_edge(s, p, o);
	
	/* Release created UTF8 string */
	env->ReleaseStringUTFChars(arg1, s);
	env->ReleaseStringUTFChars(arg2, p);
	env->ReleaseStringUTFChars(arg3, o);
	
	return rc;
}

JNIEXPORT jint JNICALL Java_ghirl_persistance_Hexastore_addAProp(JNIEnv *env, jobject obj, jstring arg1, jstring arg2, jstring arg3) {
	open_DB();
	/* Convert to UTF8 */
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	const char *p  = env->GetStringUTFChars(arg2, JNI_FALSE);
	const char *o  = env->GetStringUTFChars(arg3, JNI_FALSE);

	/* Call into external dylib function */
	jint rc = put_prop(s, p, o);
	
	/* Release created UTF8 string */
	env->ReleaseStringUTFChars(arg1, s);
	env->ReleaseStringUTFChars(arg2, p);
	env->ReleaseStringUTFChars(arg3, o);
	
	return rc;
}

JNIEXPORT jint JNICALL Java_ghirl_persistance_Hexastore_addANode(JNIEnv *env, jobject obj, jstring arg1, jstring arg2) {
	open_DB();
	/* Convert to UTF8 */
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	const char *p  = env->GetStringUTFChars(arg2, JNI_FALSE);

	
	/* Call into external dylib function */
	jint rc = make_node(s, p);
	
	/* Release created UTF8 string */
	env->ReleaseStringUTFChars(arg1, s);
	env->ReleaseStringUTFChars(arg2, p);
	
	return rc;
}

JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Hexastore_getSet(JNIEnv *env, jobject obj, jstring arg1, jstring arg2) {
	open_DB();
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	const char *p  = env->GetStringUTFChars(arg2, JNI_FALSE);
	
	btree_data *os = get_from(s, p);
	if (os->tail_size < 0){
		delete os;
		return NULL;
	}
	jobjectArray result = env->NewObjectArray(os->tail_size, env->FindClass("java/lang/String"), 0);
	
	
	int i = 0;
	while (i < os->tail_size){
		// mapping
		string st = s_mapping(*mapDB, os->tail[i]);
		jstring js = env->NewStringUTF(st.c_str());
		 env->SetObjectArrayElement(result, i, js);
		++i;
	}

	delete os;
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Hexastore_getLabels(JNIEnv *env, jobject obj, jstring arg1){
	open_DB();
	if (DEBUG == 1)
		cout << "[GETLABELS]  call" << endl;
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	
	btree_data *os = get_from("", s);
	if (DEBUG == 1){
		cout << "[OS_TAIL] " << os->tail_size  << endl;
	}
	if (os->tail_size < 0){
		delete os;
		if (DEBUG == 1){
			cout << "[GET_LABELS_NULL_END]" << endl;
		}
		return NULL;
	}
	jobjectArray result = env->NewObjectArray(os->tail_size, env->FindClass("java/lang/String"), 0);
	
	int i = 0;
	
	while (i < os->tail_size){
		string st = s_mapping(*mapDB, os->tail[i]);
		jstring js = env->NewStringUTF(st.c_str());
		env->SetObjectArrayElement(result, i, js);
		++i;
	}
	delete os;
	if (DEBUG == 1){
		cout << "[GET_LABELS_END]" << endl;
	}
	return result;
}

JNIEXPORT jstring JNICALL Java_ghirl_persistance_Hexastore_getProp(JNIEnv *env, jobject obj, jstring arg1, jstring arg2 ){
	open_DB();
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	const char *p  = env->GetStringUTFChars(arg2, JNI_FALSE);
	string t = get_prop(s,p);
	
	jstring js = env->NewStringUTF(t.c_str());
	return js;

}



JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Hexastore_getNodes(JNIEnv *env, jobject obj){
	if (DEBUG == 1){
		cout << "[GET_NODES_START]" << endl;
	}
	open_DB();
	btree_data *bd = read_nodes(*mymdb);
	jobjectArray result = env->NewObjectArray(bd->tail_size, env->FindClass("java/lang/String"), 0);

	int i = 0;
	while (i < bd->tail_size){

		string st = s_mapping(*mapDB,bd->tail[i]);
		jstring js = env->NewStringUTF(st.c_str());
		env->SetObjectArrayElement(result, i, js);
		++i;
	}
	
	delete bd;
	if (DEBUG == 1){
		cout << "[GET_NODES_END]" << endl;
	}
	return result;
}

JNIEXPORT jint JNICALL Java_ghirl_persistance_Hexastore_containsNode(JNIEnv *env, jobject obj, jstring arg1){
	if (DEBUG == 1){
		cout << "[CONTAINS_NODE_START]" << endl;
	}
	open_DB();
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	jint rc = contains_node(s);
	env->ReleaseStringUTFChars(arg1, s);
	if (DEBUG == 1){
		cout << "[CONTAINS_NODE_END]" << endl;
	}
	return rc;
}

JNIEXPORT jstring JNICALL Java_ghirl_persistance_Hexastore_getNode(JNIEnv *env, jobject obj, jstring arg1) {
	if (DEBUG == 1){
		cout << "[GET_NODE_START]" << endl;
	}
	/* Convert to UTF8 */
	open_DB();
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);

	/* Call into external dylib function */
	jint rc = contains_node(s);
	string st = "";
	if (rc > 0){
		int nid = *(nodes[s_id[s]].begin());

		st = id_s[nid];
		jstring js = env->NewStringUTF(st.c_str());
		env->ReleaseStringUTFChars(arg1, s);
		if (DEBUG == 1){
			cout << "[GET_NODE_END]" << endl;
		}
		return js;
	}
	env->ReleaseStringUTFChars(arg1, s);
	if (DEBUG == 1){
		cout << "[GET_NODE_END]" << endl;
	}
	return NULL;
		
}

JNIEXPORT void JNICALL Java_ghirl_persistance_Hexastore_writeAll(JNIEnv *env, jobject obj){
	open_DB();
	write_all_db(*mymdb);
	
}

JNIEXPORT void JNICALL Java_ghirl_persistance_Hexastore_setBase(JNIEnv *env, jobject obj, jstring bname){
	const char *cArr = env->GetStringUTFChars(bname, JNI_FALSE);
	dict_db = string(cArr) + "_dict.db";
	data_db = string(cArr) + "_data.db";
	
}

JNIEXPORT void JNICALL Java_ghirl_persistance_Hexastore_open(JNIEnv *env, jobject obj, jstring mode){
	const char *m = env->GetStringUTFChars(mode, JNI_FALSE);
	open_DB(m);
}

JNIEXPORT void JNICALL Java_ghirl_persistance_Hexastore_close(JNIEnv *env, jobject obj){
	if (mapDB != NULL && mymdb != NULL ) {
		mapDB->close();
		mymdb->close();
		delete mapDB;
		delete mymdb;
		mapDB = NULL;
		mymdb = NULL;
	}
}
