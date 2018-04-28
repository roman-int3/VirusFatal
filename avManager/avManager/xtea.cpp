#include "stdafx.h"


void XTEA_enc (unsigned int num_rounds, unsigned long* v, unsigned long* k) 
{ 
    unsigned long v0 = v[0], v1 = v[1], i; 
    unsigned long sum = 0, delta = 0x9E3779B9; 

    for(i = 0; i < num_rounds; i++) 
   { 
        v0  += ((v1 << 4 ^ v1 >> 5) + v1) ^ (sum + k[sum & 3]); 
        sum += delta; 
        v1  += ((v0 << 4 ^ v0 >> 5) + v0) ^ (sum + k[sum >> 11 & 3]); 
    } 
    v[0] = v0; 
   v[1] = v1; 
} 


void XTEA_dec (unsigned int num_rounds, unsigned long* v, unsigned long* k) 
{ 
    unsigned long v0 = v[0], v1 = v[1], i; 
    unsigned long delta = 0x9E3779B9, sum = delta * num_rounds; 

    for(i = 0; i < num_rounds; i++) 
   { 
        v1  -= ((v0 << 4 ^ v0 >> 5) + v0) ^ (sum + k[sum >> 11 & 3]); 
        sum -= delta; 
        v0  -= ((v1 << 4 ^ v1 >> 5) + v1) ^ (sum + k[sum & 3]); 
    } 
    v[0] = v0; 
   v[1] = v1; 
} 


DWORD xteaEncode (char *in, void *out,int len, char *pwd) 
{ 
	char mPwd[16];
	
	RtlZeroMemory(mPwd,sizeof(mPwd));
	RtlMoveMemory(mPwd,pwd,lstrlen(pwd));
	
	int totalLen = len+4;
	while ((totalLen%8) != 0) totalLen++;
 
	*(DWORD*)out = (DWORD)GlobalAlloc(GPTR,totalLen);
	*(int*)*(int*)out = len;
	RtlMoveMemory((void*)(*(DWORD*)out+4),in,len); 
	for(int i=0;i<totalLen;i+=8)
		XTEA_enc(64, (unsigned long*)(char*)(*(int*)out+i), (unsigned long*)mPwd); 
	return totalLen;	
} 


DWORD xteaDecode (unsigned char *in, void *out,int len, const char *pwd) 
{ 
	char mPwd[16];
	
	RtlZeroMemory(mPwd,sizeof(mPwd));
	RtlMoveMemory(mPwd,pwd,lstrlen(pwd));
	
	for(int i=0; i < len; i+=8)
	   XTEA_dec(64, (unsigned long*)&in[i], (unsigned long*)mPwd); 
	
	*(DWORD*)out = (DWORD)GlobalAlloc(GPTR,*(DWORD*)in+1);
	RtlMoveMemory((void*)(*(DWORD*)out),&in[4],*(DWORD*)in); 
	   
  return *(DWORD*)in;
}