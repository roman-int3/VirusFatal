#include "hook_dll.h"



void* MyRealloc(void *buf,int len)
{
	HANDLE hProcHeap = GetProcessHeap();
	if(buf == 0)
		return (void *)HeapAlloc(hProcHeap,HEAP_ZERO_MEMORY,len+1);
	return (void *)HeapReAlloc(hProcHeap,HEAP_ZERO_MEMORY,buf,HeapSize(hProcHeap,0,buf)+len);
}


void MyFree(void *buf)
{
	if(buf != 0)HeapFree(GetProcessHeap(),HEAP_NO_SERIALIZE,buf);
}


WCHAR* CharToWChar(char* szString)
{
	int Size = lstrlen(szString)+2;
	WCHAR* wData = (WCHAR*)GlobalAlloc(GPTR,Size*2);
	MultiByteToWideChar(CP_ACP,0,szString,-1,wData,Size);
	return wData;
}


char* WCharToChar(WCHAR* wString)
{
	int Size = lstrlenW(wString)+1;
	char* wData = (char*)GlobalAlloc(GPTR,Size);
	WideCharToMultiByte(CP_ACP,0,wString,-1,wData,Size,0,0);
	return wData;
}


static unsigned long x=123456789,y=362436069,z=521288629,w=88675123;
unsigned long _random()
{
	unsigned long t,rez;
	_asm pushad;
	t=(x^(x<<11));
	x=y; y=z; z=w;
	rez = (w=(w^(w>>19))^(t^(t>>8)));
	_asm popad;
	return rez;
}


DWORD WINAPI _prandom(DWORD _min,DWORD _max)
{
	_asm
	{
		push ebx
		push edx
		mov     ebx, _max
		sub     ebx, _min
		inc     ebx
		xor     edx, edx
		call	_random
		div     ebx
		add     edx, _min
		mov     eax, edx
		pop edx
		pop ebx
	}
}


DWORD WINAPI _rand(DWORD _max)
{
	_asm
	{
		push ecx
		push edx
        mov eax,_max
        imul	eax,eax,100	
        push eax
		call _random
		pop ecx
        xor edx,edx
        div ecx
        xchg	eax,edx
        xor	edx,edx
		push	100
		pop	ecx		
		div	ecx	
		pop edx
		pop ecx
	};
}


DWORD writeFile(char *szfPath,char *Data,int DataSize,int CreationDisposition,int Attr)
{
	DWORD rez = 0;
	
	HANDLE hFile = CreateFile(szfPath,GENERIC_READ|GENERIC_WRITE,FILE_SHARE_READ,0,CreationDisposition,Attr,0);
	if(hFile != INVALID_HANDLE_VALUE)
	{
		SetFilePointer(hFile,0,0,FILE_END);
		WriteFile(hFile,Data,DataSize,&rez,0);
		CloseHandle(hFile);
	}
	
	return rez;
}





unsigned int WINAPI CalcHash(char *string)
{
	unsigned int hash = 0;
	int i=0;
	for(;string[i] != 0;i++)
	{
		hash = (((hash << 7) & 0xFFFFFFFF) | (hash >> (32-7))) ^ (unsigned char)string[i];
	}

	return hash;
}

void writeLogFile(char *Data)
{

	DWORD rez;
	int dataSize = lstrlen(Data);
	HANDLE hFile = CreateFile("c:\\hookdlllog.txt",GENERIC_READ|GENERIC_WRITE,FILE_SHARE_READ,0,OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL,0);
	if(hFile != INVALID_HANDLE_VALUE)
	{
		SetFilePointer(hFile,0,0,FILE_END);
		WriteFile(hFile,Data,dataSize,&rez,0);
		WriteFile(hFile,"\r\n",2,&rez,0);
		CloseHandle(hFile);
	}


}

void writeLogFile(void* Data,int dataSize)
{

	DWORD rez;
	HANDLE hFile = CreateFile("c:\\hookdlllog.txt",GENERIC_READ|GENERIC_WRITE,FILE_SHARE_READ,0,OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL,0);
	if(hFile != INVALID_HANDLE_VALUE)
	{
		SetFilePointer(hFile,0,0,FILE_END);
		WriteFile(hFile,Data,dataSize,&rez,0);
		CloseHandle(hFile);
	}
}
