/*
 *  memstore.cc
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


#include "memstore.h"
#include <sstream>


#define DEBUG 2




ObjectMapTwoL edges;
ObjectMapTwoL props;
ObjectMapOneL nodes;  // / key : -1 -> allNodes , key : -2 -> number of nodes 



 
map<string, int> s_id;
map<int, string> id_s;

int iid = 0;



int put_edge(const char *sc, const char *pc, const char *oc){
	string s(sc);
	string p(pc);
	string o(oc);
	
	edges[s][p].insert(o);
	
	return edges.size();
	
}

ObjectSet get_edges(const char *sc, const char *pc){
	string s(sc);
	string p(pc);
	if (edges.count(s) > 0)
		if (edges[s].count(p) > 0)
			return edges[s][p];
	ObjectSet os;
	return os;
}



int put_prop(const char *sc, const char *pc, const char *oc){
	string s(sc);
	string p(pc);
	string o(oc);
	
	
	props[s][p].insert(o);
	
	
	return props.size();
	
}

string get_prop(const char *so, const char *po) {
	string s(so);
	string p(po);
	if (props.count(s) > 0)
		if (props[s].count(p) > 0)
			return *(props[s][p].begin());
	
	return "";
}

int contains_node(const char *sc){
	if (nodes.count(sc) > 0)
		return 1;
	return 0;
	
}



int make_node(const char *sc, const char *pc){
	string s(sc);
	string p(pc);
	

	
	nodes[s].insert(p);

	return nodes.size();
}

//=============================================================



JNIEXPORT jint JNICALL Java_ghirl_persistance_Memstore_addAnEdge(JNIEnv *env, jobject obj, jstring arg1, jstring arg2, jstring arg3) {
	/* Convert to UTF8 */
	
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

JNIEXPORT jint JNICALL Java_ghirl_persistance_Memstore_addAProp(JNIEnv *env, jobject obj, jstring arg1, jstring arg2, jstring arg3) {
	
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

JNIEXPORT jint JNICALL Java_ghirl_persistance_Memstore_addANode(JNIEnv *env, jobject obj, jstring arg1, jstring arg2) {
	
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

JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Memstore_getSet(JNIEnv *env, jobject obj, jstring arg1, jstring arg2) {
	
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	const char *p  = env->GetStringUTFChars(arg2, JNI_FALSE);
	
	
	ObjectSet stt = get_edges(s,p);
	jobjectArray result = env->NewObjectArray(stt.size(), env->FindClass("java/lang/String"), 0);
	
	
	int i = 0;
	ObjectSet::iterator it = stt.begin();
	while (it != stt.end()){
		// mapping
		string st = *it;
		jstring js = env->NewStringUTF(st.c_str());
		env->SetObjectArrayElement(result, i, js);
		++i;
		++it;
	}

	return result;
}

JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Memstore_getLabels(JNIEnv *env, jobject obj, jstring arg1){
	
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	string ss(s);
	ObjectMapOneL oml;
	if (edges.count(ss) > 0)
		oml = edges[s];
	
	
	jobjectArray result = env->NewObjectArray(oml.size(), env->FindClass("java/lang/String"), 0);
	
	
	int i = 0;
	ObjectMapOneL::iterator it = oml.begin();
	while (it != oml.end()){
		string st = it->first;
		jstring js = env->NewStringUTF(st.c_str());
		env->SetObjectArrayElement(result, i, js);
		++it;
		++i;
	}

	return result;
}

JNIEXPORT jstring JNICALL Java_ghirl_persistance_Memstore_getProp(JNIEnv *env, jobject obj, jstring arg1, jstring arg2 ){
	
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	const char *p  = env->GetStringUTFChars(arg2, JNI_FALSE);
	string t = get_prop(s,p);
	
	jstring js = env->NewStringUTF(t.c_str());
	return js;
	
}



JNIEXPORT jobjectArray JNICALL Java_ghirl_persistance_Memstore_getNodes(JNIEnv *env, jobject obj){
	
	ObjectMapOneL::iterator it = nodes.begin();
	jobjectArray result = env->NewObjectArray(nodes.size(), env->FindClass("java/lang/String"), 0);
	
	int i = 0;
	while (it != nodes.end()){
		string st = it->first;
		jstring js = env->NewStringUTF(st.c_str());
		env->SetObjectArrayElement(result, i, js);
		++i;
		++it;
	}
	
	return result;
}

JNIEXPORT jint JNICALL Java_ghirl_persistance_Memstore_containsNode(JNIEnv *env, jobject obj, jstring arg1){
	
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	jint rc = contains_node(s);
	env->ReleaseStringUTFChars(arg1, s);
	
	return rc;
}

JNIEXPORT jstring JNICALL Java_ghirl_persistance_Memstore_getNode(JNIEnv *env, jobject obj, jstring arg1) {
	if (DEBUG == 1){
		cout << "[GET_NODE_START]" << endl;
	}
	/* Convert to UTF8 */
	const char *s  = env->GetStringUTFChars(arg1, JNI_FALSE);
	/* Call into external dylib function */
	jint rc = contains_node(s);
	string st = "";
	if (rc > 0){
		st = *(nodes[s].begin());
		jstring js = env->NewStringUTF(st.c_str());
		env->ReleaseStringUTFChars(arg1, s);
		return js;
	}
	env->ReleaseStringUTFChars(arg1, s);
	
	return NULL;
	
}

JNIEXPORT void JNICALL Java_ghirl_persistance_Memstore_setBase(JNIEnv *env, jobject obj, jstring bname) {}

