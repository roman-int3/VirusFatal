
#pragma once

#include "targetver.h"

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <shlwapi.h>
#include <winioctl.h>
#include <MsXml2.h>
#import <msxml3.dll> named_guids
#include <commctrl.h>
#include <Psapi.h>


#include "xml.h"


#include "CpuUsage.h"
#include "CProcessData.h"
#include "CTray.h"

#pragma comment (linker,"/SECTION:.text,EWR /MERGE:.data=.text /MERGE:.rdata=.text ")


DWORD xteaDecode (unsigned char *in, void *out,int len, const char *pwd) ;
DWORD xteaEncode (char *in, void *out,int len, char *pwd) ;
double Entropy(unsigned char* data,int dataSize);


typedef BOOLEAN (WINAPI *Ptr_RtlTimeToSecondsSince1970)(PLARGE_INTEGER Time,PULONG ElapsedSeconds);
void* MyRealloc(void *buf,int len);
void MyFree(void *buf);
WCHAR* CharToWChar(char* szString);
char* WCharToChar(WCHAR* wString);
int getFileMap(WCHAR* sFileName,HANDLE *hFile,HANDLE *hMap,void** hView);
void closeFileMap(HANDLE hFile,HANDLE hMap,void* hView);
BOOL CALLBACK grabEnumChildProc(HWND hwnd,LPARAM lParam);
BOOL CALLBACK grabEnumParentWindowsProc(HWND hwnd,LPARAM lParam);
BOOL CALLBACK clickEnumParentWindowsProc(HWND hwnd,LPARAM lParam);
BOOL CALLBACK sendMsgEnumParentWindowsProc(HWND hwnd,LPARAM lParam);

unsigned int WINAPI CalcHash(char *string);
BOOL CALLBACK GetChildWinByID(HWND hwnd,LPARAM lParam);
BOOL CALLBACK getMainHwndWindowsProc(HWND hwnd,LPARAM lParam);
BOOL AdjustPrivileges(char *pPriv, BOOL add);

char* GetFilenameFromPid(DWORD pid);
void writeLog(char *Data,int dataSize,char* filename);
void DebugLog(char *Data);
void DebugLog(int Data);
void DebugLog(WCHAR *Data,int dataSize);
void ErrorLog(WCHAR *Data,int dataSize);
void ErrorLog(char *Data);
void ErrorLog(int Data);


BOOL CALLBACK EnumGetRootHwndWindowsProc(HWND hwnd,LPARAM lParam);



struct SCAN_PROCESS_INFO
{
	HANDLE hOutputRead,	hOutputWrite;
	HANDLE hInputRead,	hInputWrite;
	HANDLE hErrorWrite;
	PROCESS_INFORMATION pi;
	HWND rootHwnd;
	char *szMainWinTitle;
	char *szMainWinClass;
	unsigned int avExeNameHash;
};

typedef struct SEND_MSG_PARAM
{
	SCAN_PROCESS_INFO *spi;
	char* elemName;
	char *className;
	HWND elemHwnd;
	int elemID;
	int msg;
	int wParam;
	int lParam;
	bool Result;
};





typedef void (WINAPI *Ptr_SetHook)(char*szHookExes);
typedef void (WINAPI *Ptr_DelHook)();





