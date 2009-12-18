/*
 *  memstore.h
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



#ifndef _Included_memstore
#define _Included_memstore
#ifdef __cplusplus
extern "C" {
#endif

using namespace std;

	typedef set<string> ObjectSet;
	
	typedef map<string , ObjectSet> ObjectMapOneL;
	
	typedef map<string , ObjectMapOneL> ObjectMapTwoL;
	
	typedef map<string, string> SSMap;
	
	
	
	int put_edge(const char *s, const char *p, const char *o);
	
	int put_prop(const char *s, const char *p, const char *o);
	
	int make_node(const char *s1, const char *s2);
	
	
	string get_prop(const char *s, const char *p);

	JNIEXPORT jint JNICALL Java_ghirl_persistance_Memstore_addANode(JNIEnv *env, jobject obj, jstring arg1, jstring arg2);
	
	JNIEXPORT jint JNICALL Java_ghirl_persistance_Memstore_addAnEdge(JNIEnv *env, jobject obj, jstring arg1, jstring arg2, jstring arg3);
	
	JNIEXPORT jint JNICALL Java_ghirl_persistance_Memstore_addAProp(JNIEnv *env, jobject obj, jstring arg1, jstring arg2, jstring arg3);

	JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Memstore_getSet(JNIEnv *env, jobject obj, jstring arg1, jstring arg2);
	
	JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Memstore_getNodes(JNIEnv *env, jobject obj);
	
	JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Memstore_getLabels(JNIEnv *env, jobject obj, jstring arg1);
	
	JNIEXPORT jstring JNICALL Java_ghirl_persistance_Memstore_getProp(JNIEnv *env, jobject obj, jstring arg1, jstring arg2);
	
	JNIEXPORT jint JNICALL Java_ghirl_persistance_Memstore_containsNode(JNIEnv *env, jobject obj, jstring arg1);
	
	JNIEXPORT jstring JNICALL Java_ghirl_persistance_Memstore_getNode(JNIEnv *env, jobject obj, jstring arg1);
	
	JNIEXPORT void JNICALL Java_ghirl_persistance_Memstore_writeAll(JNIEnv *env, jobject obj);

	JNIEXPORT void JNICALL Java_ghirl_persistance_Memstore_setBase(JNIEnv *env, jobject obj, jstring bname);
#ifdef __cplusplus
}
#endif
#endif
 
