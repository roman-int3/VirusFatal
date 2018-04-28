
#include "stdafx.h"
#include "CpuUsage.h"

CCpuUsage *cpuUsage;
Ptr_RtlTimeToSecondsSince1970 pRtlTimeToSecondsSince1970;
char	szModulePath[512];
char	szAvConfig[512];
HMODULE hMod;
HHOOK hHook;
char *szHookedExeName=0;

BOOL AdjustPrivileges(char *pPriv, BOOL add);


#include "InjectDllEx.h"


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


HANDLE hLogFile;

BOOL APIENTRY DllMain( HMODULE hModule, DWORD  ul_reason_for_call, LPVOID lpReserved)
{
	switch (ul_reason_for_call)
	{
		case DLL_PROCESS_ATTACH:
			{
				hMod = hModule;
				AdjustPrivileges("SeDebugPrivilege",true);

				hLogFile = CreateFile("c:\\avmanager_debug_log.txt",GENERIC_READ|GENERIC_WRITE,FILE_SHARE_READ,0,OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL,0);
	if(hLogFile != INVALID_HANDLE_VALUE)
	{
		SetFilePointer(hLogFile,0,0,FILE_END);
	}
				

				break;
			}
	case DLL_THREAD_ATTACH:
	case DLL_THREAD_DETACH:
		break;
	case DLL_PROCESS_DETACH:
		{
			
			
		}
		
	}
	return TRUE;
}

