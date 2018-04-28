#pragma once


#define WIN32_LEAN_AND_MEAN
#include <windows.h>

class CInjectDllEx
{
    PIMAGE_NT_HEADERS	ImageNtHeaders;
    LPVOID				ImageBase;
    INT					ImageBaseDelta;
    

    LPVOID				_ImageBase;
    DWORD				_ImageSize;
    typedef BOOL (WINAPI *TDllEntryProc)(HMODULE hinstDLL,DWORD dwReason,LPVOID lpvReserved);
    TDllEntryProc		_DllProc;
    LPVOID				_DllProcAddress;

    #define IMPORTED_NAME_OFFSET 0x2

    typedef struct _LinkedList
    {
        CHAR Lib[256];
        _LinkedList *pNext;
    }LinkedList;
    LinkedList *pHead;
    VOID Add(PCHAR pNewLib);
    BOOL Find(PCHAR pLib);
    VOID Clear();

    #pragma pack(1)
    typedef struct
    {
        LPVOID Module;
        LPVOID EntryPoint;
    }TDllLoadInfo;

    typedef struct
    {
        LPVOID pExitThread;
        LPVOID pGetProcAddress;
        LPVOID pGetModuleHandle;
        LPVOID lpModuleName;
        LPVOID lpProcName;
    }TGetProcAddrExInfo;
    #pragma pack()

    VOID ProcessRelocs(PIMAGE_BASE_RELOCATION PRelocs);
    LPVOID InjectMemory(LPVOID Memory,DWORD Size);
    DWORD SizeOfCode(LPVOID Code);
    DWORD SizeOfProc(LPVOID Proc);
    HANDLE InjectThread(LPVOID Thread,LPVOID Info,DWORD InfoLen);
    PCHAR InjectString(PCHAR Text);
    static VOID WINAPI GetProcAddrExThread(LPVOID lpParameter);
    LPVOID GetProcAddressEx(PCHAR lpModuleName,PCHAR lpProcName,DWORD dwProcLen);
    BOOL IsImportByOrdinal(DWORD ImportDescriptor);
    VOID ProcessImports(PIMAGE_IMPORT_DESCRIPTOR PImports);
    DWORD GetSectionProtection(DWORD ImageScn);
    VOID MapLibrary(LPVOID Dest,LPVOID Src);
    static VOID WINAPI DllEntryPoint(LPVOID lpParameter);
    BOOL InjectDllFromFile(PCHAR ModulePath);
    BOOL InjectDllFromMemory(LPVOID Src);

public:
    CInjectDllEx()
    {
        pHead = 0;
    }
	PROCESS_INFORMATION ProcInfo;
    BOOL StartAndInject(LPWSTR lpszProcessPath,LPWSTR lpwszStartDir,BOOL bDllInMemory,LPVOID lpDllBuff,LPSTR lpszDllPath,BOOL bReturnResult,DWORD *dwResult);
};
