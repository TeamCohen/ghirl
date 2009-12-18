/*
 *  hexastore.h
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
#include "jni.h"
#include <iostream>
#include <map>
#include <set>
#include <string>
#include "bdbhash/BDB/mydb.h"
#include "bdbhash/btree_marshal.cpp"


#ifndef _Included_hexastore
#define _Included_hexastore
#ifdef __cplusplus
extern "C" {
#endif

using namespace std;

	typedef set<int> ObjectSet;
	
	typedef map<int , ObjectSet> ObjectMapOneL;
	
	typedef map<int , ObjectMapOneL> ObjectMapTwoL;
	
	typedef map<int, int> SSMap;
	
	
	
	int put_edge(const char *s, const char *p, const char *o);
	
	int put_prop(const char *s, const char *p, const char *o);
	
	int make_node(const char *s1, const char *s2);
	
	btree_data *get_from(const char *s, const char *p);
	
	string get_prop(const char *s, const char *p);

	JNIEXPORT jint JNICALL Java_ghirl_persistance_Hexastore_addANode(JNIEnv *env, jobject obj, jstring arg1, jstring arg2);
	
	JNIEXPORT jint JNICALL Java_ghirl_persistance_Hexastore_addAnEdge(JNIEnv *env, jobject obj, jstring arg1, jstring arg2, jstring arg3);
	
	JNIEXPORT jint JNICALL Java_ghirl_persistance_Hexastore_addAProp(JNIEnv *env, jobject obj, jstring arg1, jstring arg2, jstring arg3);

	JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Hexastore_getSet(JNIEnv *env, jobject obj, jstring arg1, jstring arg2);
	
	JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Hexastore_getNodes(JNIEnv *env, jobject obj);
	
	JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Hexastore_getLabels(JNIEnv *env, jobject obj, jstring arg1);
	
	JNIEXPORT jstring JNICALL Java_ghirl_persistance_Hexastore_getProp(JNIEnv *env, jobject obj, jstring arg1, jstring arg2);
	
	JNIEXPORT jint JNICALL Java_ghirl_persistance_Hexastore_containsNode(JNIEnv *env, jobject obj, jstring arg1);
	
	JNIEXPORT jstring JNICALL Java_ghirl_persistance_Hexastore_getNode(JNIEnv *env, jobject obj, jstring arg1);
	
	JNIEXPORT void JNICALL Java_ghirl_persistance_Hexastore_writeAll(JNIEnv *env, jobject obj);

	JNIEXPORT void JNICALL Java_ghirl_persistance_Hexastore_setBase(JNIEnv *env, jobject obj, jstring bname);

#ifdef __cplusplus
}
#endif
#endif
 
