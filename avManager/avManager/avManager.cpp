
#include "stdafx.h"
#include "AvCheckManager.h"
#include "CpuUsage.h"
#include "externs.h"
#include "InjectDllEx.h"
#include "unknwn.h"
#include <ShellAPI.h>
#include <ShlObj.h>
#include "DataObject.h"



int IpcSend(char *szMessage,char **szResponce,char *szPipeName,char *szAdditionData=0)
{
	int RespSize = 0;

	HANDLE hPipe = CreateFile(szPipeName,GENERIC_READ | GENERIC_WRITE ,0,0,OPEN_EXISTING,0,0);
	if(hPipe != INVALID_HANDLE_VALUE)
	{
		DWORD dwMode = PIPE_READMODE_BYTE; 
		BOOL fSuccess = SetNamedPipeHandleState(hPipe,&dwMode,NULL,NULL);

		DWORD bw;
		WriteFile(hPipe,szMessage,lstrlen(szMessage),&bw,0);
		if(szAdditionData != 0)
		{
			WriteFile(hPipe,"/",1,&bw,0);
			WriteFile(hPipe,szAdditionData,lstrlen(szAdditionData),&bw,0);
		}
		
		WriteFile(hPipe,"/END_PACKET",11,&bw,0);
		FlushFileBuffers(hPipe);
		
		if(szResponce)
		{
			*szResponce = (char*)MyRealloc(0,1024);
			while(ReadFile(hPipe,*szResponce+RespSize,1024,&bw,0) && bw > 0)				
			{
				RespSize += bw;
				*szResponce = (char*)MyRealloc(*szResponce,1024);
			}
			
			if(RespSize == 0)
			{
				MyFree(*szResponce);
				*szResponce = 0;
			}
		}

		CloseHandle(hPipe);
		return RespSize;
	}
	else
	{

	}

	return -1;
}


int IpcSendMessage(char *szMessage,char **szResponce,SCAN_PROCESS_INFO *spi,char *szAdditionData=0)
{
	char szPipeName[512];
	int RespSize = 0;

	if(spi->pi.dwProcessId)
	{
		wsprintf(szPipeName,"\\\\.\\pipe\\%u",spi->pi.dwProcessId);
		RespSize = IpcSend(szMessage,szResponce,szPipeName,szAdditionData);
	}
	
	if(RespSize == -1)
	{
		wsprintf(szPipeName,"\\\\.\\pipe\\%u",spi->avExeNameHash);
		RespSize = IpcSend(szMessage,szResponce,szPipeName,szAdditionData);
	}

	OutputDebugString("IpcSendMessage:");
	OutputDebugString(szMessage);
	OutputDebugString(szPipeName);

	return (RespSize == -1)?0:RespSize;
}


jbyteArray JNICALL Java_CheckManager_JNI_loadAvConfig(JNIEnv *env, jclass jobj)
{
	#ifdef _DEBUG 
		HANDLE hFile = CreateFile(szAvConfig,GENERIC_READ,FILE_SHARE_READ,0,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,0);
	#else
	
		HANDLE hFile = CreateFile(szAvConfig,GENERIC_READ,FILE_SHARE_READ,0,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,0);
	#endif

		if(hFile == INVALID_HANDLE_VALUE)
		{
			env->ThrowNew(jobj,"avscan.xml missing");	
		}

		int fSize = GetFileSize(hFile,0);
		char *buf1 = (char *)GlobalAlloc(GPTR,fSize+2);
		DWORD br;
		ReadFile(hFile,buf1,fSize,&br,0);
		CloseHandle(hFile);
		
#ifdef _DEBUG
		jbyteArray retArr = env->NewByteArray(fSize);
		env->SetByteArrayRegion(retArr,0,fSize,(const jbyte *)buf1);
		GlobalFree(buf1);
#else
		char *OutData=0;


		jbyteArray retArr = env->NewByteArray(fSize);
		env->SetByteArrayRegion(retArr,0,fSize,(const jbyte *)OutData);
		GlobalFree(OutData);
		GlobalFree(buf1);
#endif

		return retArr;
 }	


jint JNICALL Java_CheckManager_JNI_getCpuUsage(JNIEnv *env, jclass jobj)
{
	return cpuUsage->GetCpuUsage();
}






jint JNICALL Java_CheckManager_JNI_preCreateScanProcess(JNIEnv *env, jclass jobj, jstring sFileName)
{
	char tmpBuf[250];
	STARTUPINFO si;

	jboolean isCopy=false;
	WCHAR *sFileBuf =(WCHAR*)env->GetStringChars(sFileName,&isCopy);
	WideCharToMultiByte(CP_ACP,0,(LPCWSTR)sFileBuf,-1,tmpBuf,250,0,0);
	env->ReleaseStringChars(sFileName,(const jchar *)sFileBuf);

	PROCESS_INFORMATION *pInfo = (PROCESS_INFORMATION*)GlobalAlloc(GPTR,sizeof(PROCESS_INFORMATION));

	ZeroMemory(&si,sizeof(STARTUPINFO));
	si.cb = sizeof(STARTUPINFO);
	BOOL result = CreateProcess(NULL,tmpBuf,NULL,NULL,TRUE,CREATE_SUSPENDED|CREATE_NO_WINDOW,NULL,NULL,&si,pInfo);
	if(result)
	{
		CloseHandle(pInfo->hThread);
		return (int)pInfo;
	}
	else
		GlobalFree(pInfo);

	return 0;
}


void JNICALL Java_CheckManager_JNI_preCreateScanProcessClose(JNIEnv *env, jclass jobj, jint pi)
{
	PROCESS_INFORMATION *pInfo = (PROCESS_INFORMATION*)pi;
	TerminateProcess(pInfo->hProcess,0);
	GlobalFree(pInfo);
}


jint JNICALL Java_CheckManager_JNI_createNewInjectedScanProcess(JNIEnv *env , jclass jobj, jstring avCmd,jstring avStartDir)
{

	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO *)GlobalAlloc(GPTR,sizeof(SCAN_PROCESS_INFO));
	WCHAR *wszCmd =(WCHAR*)env->GetStringChars(avCmd,0);
	WCHAR *wszStartDir =(WCHAR *)env->GetStringChars(avStartDir,0);

	STARTUPINFOW StartInfo;

    ZeroMemory(&StartInfo,sizeof(STARTUPINFO));
    StartInfo.cb = sizeof(STARTUPINFO);
    StartInfo.dwFlags = STARTF_USESHOWWINDOW;
    StartInfo.wShowWindow = SW_SHOWMAXIMIZED;
    if(wszStartDir[0] == 0 && wszStartDir[1] == 0) wszStartDir = 0;
	CreateProcessW(0,wszCmd,0,0,FALSE,NORMAL_PRIORITY_CLASS,0,wszStartDir,&StartInfo,&spi->pi);
    
	env->ReleaseStringChars(avStartDir,(const jchar *)wszStartDir);
	env->ReleaseStringChars(avCmd,(const jchar *)wszCmd);

	return (int)spi;
}


jstring JNICALL Java_CheckManager_JNI_getInjectedGrabText(JNIEnv *env, jclass jobj, jint pi)
{
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)pi;

	if(spi)
	{
		char *szResponce;
		
		if(spi->rootHwnd == 0)
		{
			EnumWindows(EnumGetRootHwndWindowsProc,(LPARAM)spi);
			DebugLog("getInjectedGrabText: spi->rootHwnd == 0; spi->rootHwnd = ");
			DebugLog((int)spi->rootHwnd);
		}

		if(spi->rootHwnd)
		{
			BringWindowToTop(spi->rootHwnd);
			SetForegroundWindow(spi->rootHwnd);

		}

		int respSize = IpcSendMessage("GetText",&szResponce,spi);

		if(respSize > 0)
		{
			WCHAR *uStr = CharToWChar(szResponce);
			MyFree(szResponce);
			jstring retData = env->NewString((jchar *)uStr,lstrlenW(uStr));
			GlobalFree(uStr);
			return retData;
		}
	}

	return NULL;
}


void JNICALL Java_CheckManager_JNI_dropInjectedGrabText(JNIEnv *env, jclass jobj, jint pi)
{
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)pi;
	
	if(spi)
	{
		int status = IpcSendMessage("DropGrabText",0,spi);
	}
}




#define EXEC_PIPE 2

jint JNICALL Java_CheckManager_JNI_createNewScanProcess(JNIEnv *env , jclass jobj, jstring avCmd,jstring avStartDir,jint execType)
{
	HANDLE				hOutputReadTmp;
	HANDLE				hInputWriteTmp;
	STARTUPINFO			si;
	SECURITY_ATTRIBUTES sa;
	BOOL				result = false;
	BOOL                bInheritHandles = false;


	WCHAR *szwCmd =(WCHAR*)env->GetStringChars(avCmd,0);
	WCHAR *szwStartDir =(WCHAR *)env->GetStringChars(avStartDir,0);
	char *szCmd = WCharToChar(szwCmd);
	char *szStartDir = WCharToChar(szwStartDir);

	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO *)GlobalAlloc(GPTR,sizeof(SCAN_PROCESS_INFO));

	ZeroMemory(&si,sizeof(STARTUPINFO));
	si.cb = sizeof(STARTUPINFO);

	

	if(execType == EXEC_PIPE)
	{
		bInheritHandles = TRUE;
		
		ZeroMemory(&sa,sizeof(SECURITY_ATTRIBUTES));
		sa.nLength= sizeof(SECURITY_ATTRIBUTES);
		sa.lpSecurityDescriptor = NULL;
		sa.bInheritHandle = TRUE;
	
		CreatePipe(&hOutputReadTmp,&spi->hOutputWrite,&sa,0);
		DuplicateHandle(GetCurrentProcess(),spi->hOutputWrite,GetCurrentProcess(),&spi->hErrorWrite,0,TRUE,DUPLICATE_SAME_ACCESS);
		CreatePipe(&spi->hInputRead,&hInputWriteTmp,&sa,0);
		DuplicateHandle(GetCurrentProcess(),hOutputReadTmp,GetCurrentProcess(),&spi->hOutputRead,0,FALSE,DUPLICATE_SAME_ACCESS);
		DuplicateHandle(GetCurrentProcess(),hInputWriteTmp,GetCurrentProcess(),&spi->hInputWrite,0,FALSE,DUPLICATE_SAME_ACCESS);
		CloseHandle(hOutputReadTmp);
		CloseHandle(hInputWriteTmp);

		ZeroMemory(&si,sizeof(STARTUPINFO));
		si.cb = sizeof(STARTUPINFO);
		si.dwFlags = STARTF_USESTDHANDLES | STARTF_USESHOWWINDOW;
		si.hStdInput  = spi->hInputRead;
		si.hStdOutput = spi->hOutputWrite;
		si.hStdError  = spi->hErrorWrite;
		
	si.wShowWindow = SW_HIDE;

		result = CreateProcess(NULL,szCmd,&sa,&sa,TRUE,CREATE_NO_WINDOW,NULL,szStartDir,&si,&spi->pi);

		CloseHandle(spi->hOutputWrite);
		CloseHandle(spi->hInputRead );
		CloseHandle(spi->hErrorWrite);

		spi->hOutputWrite = 0;
		spi->hInputRead = 0;
		spi->hErrorWrite = 0;

	}
	else
	{

	result = CreateProcess(NULL,szCmd,NULL,NULL,FALSE,NULL,NULL,szStartDir,&si,&spi->pi);

	}
	
	
	GlobalFree(szCmd);
	GlobalFree(szStartDir);
	env->ReleaseStringChars(avStartDir,(const jchar *)szwStartDir);
	env->ReleaseStringChars(avCmd,(const jchar *)szwCmd);
	if(!result)
	{
		GlobalFree(spi);
		return 0;
	}
		
	CloseHandle(spi->pi.hThread);
	return (int)spi;
}

#include <Tlhelp32.h>
#define SCRATCH_QCM_FIRST 1
#define SCRATCH_QCM_LAST  0x7FFF


BOOLEAN createNewShellScanProcess(WCHAR *szwAvCLSID,WCHAR *szwCheckFilePath,int menuID,SCAN_PROCESS_INFO *spi)
{
	char					tmpBuf[512];
	IContextMenu			*pIcon;
	IShellExtInit			*ishel;
	ITEMIDLIST				*itemlist;
	IShellFolder			*iFolder;
	IMalloc					*pMalloc;
	ULONG					pchEaten,pdwAttr;
	CMINVOKECOMMANDINFOEX	pcmd;
	CLSID					pclsid;
	HRESULT					hr;
	BOOL					result = false;
	HANDLE hProcessSnap;
	PROCESSENTRY32 pe32;

	CDataObject *cdobj = new CDataObject(szwCheckFilePath);
	IDataObject *pDataObject = cdobj;

	RtlZeroMemory(&pcmd,sizeof(CMINVOKECOMMANDINFOEX));
	if(CLSIDFromString(szwAvCLSID,&pclsid) == S_OK)
	{
		if(SHGetDesktopFolder(&iFolder) == S_OK)
		{ 
			if(SHGetMalloc(&pMalloc) == S_OK)
			{
				if(iFolder->ParseDisplayName(0,0,szwCheckFilePath,&pchEaten,&itemlist,&pdwAttr) == S_OK)
				{
					hr=CoCreateInstance(pclsid,
										NULL,
										CLSCTX_INPROC_SERVER | CLSCTX_LOCAL_SERVER | CLSCTX_INPROC_HANDLER,
										IID_IShellExtInit,
										(void**)&ishel);
					if(hr == S_OK)
					{
						if(ishel->Initialize(itemlist,pDataObject,HKEY_CLASSES_ROOT) == S_OK)
						{
							if(ishel->QueryInterface(IID_IContextMenu,(void**)&pIcon) == S_OK)
							{
								HMENU hmenu = CreatePopupMenu();
								hr = pIcon->QueryContextMenu(hmenu,0,SCRATCH_QCM_FIRST,SCRATCH_QCM_LAST,CMF_EXPLORE);

								WCHAR* pDirectory = PathFindFileNameW(szwCheckFilePath);
								if(pDirectory)
								{
									int dirSize = (DWORD)pDirectory-(DWORD)szwCheckFilePath;
									WCHAR *wszDirectory = (WCHAR*)GlobalAlloc(GPTR,dirSize+2);
									RtlMoveMemory(wszDirectory,szwCheckFilePath,dirSize);
									pcmd.cbSize			= sizeof(CMINVOKECOMMANDINFOEX);
									pcmd.fMask			= CMIC_MASK_UNICODE;
									pcmd.lpParametersW	= szwCheckFilePath;
									pcmd.lpVerb			= MAKEINTRESOURCE(menuID);
									pcmd.lpDirectoryW	= wszDirectory;
									pcmd.nShow			= SW_SHOWNORMAL;
									pcmd.hwnd			= GetDesktopWindow();

									hr = pIcon->InvokeCommand((CMINVOKECOMMANDINFO*)&pcmd);
									if(hr == S_OK)
									{
										result = true;
										

										hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
										pe32.dwSize = sizeof( PROCESSENTRY32 );
										Process32First( hProcessSnap, &pe32 );
										
										do
										{
											CharLowerBuff(pe32.szExeFile,lstrlen(pe32.szExeFile));
											if(spi->avExeNameHash == CalcHash(pe32.szExeFile))
											{
												spi->pi.dwProcessId = pe32.th32ProcessID;

												break;
											}
										} while( Process32Next( hProcessSnap, &pe32 ) );

										CloseHandle( hProcessSnap );

									}
									else
									{
										ErrorLog("createNewShellScanProcess ERROR pIcon->InvokeCommand");
										wsprintf(tmpBuf,"hResult = 0x%X",hr);
										ErrorLog(tmpBuf);
									}
	
									GlobalFree(wszDirectory);
								}
								else
								{
									ErrorLog("createNewShellScanProcess ERROR PathFindFileNameW");
								}
							
								pIcon->Release();
								DestroyMenu(hmenu);
							}
							else
							{
								ErrorLog("createNewShellScanProcess ERROR ishel->QueryInterface");
							}
						}
						else
						{
							ErrorLog("createNewShellScanProcess ERROR ishel->Initialize");
						}
					
						ishel->Release();
					}
					else
					{
						wsprintf(tmpBuf,"createNewShellScanProcess ERROR CoCreateInstance - ERROR_CODE=%X",hr);
						ErrorLog(tmpBuf);
					}
				
					pMalloc->Free(itemlist);
				}
				else
				{
					ErrorLog("createNewShellScanProcess ERROR iFolder->ParseDisplayName");
				}

				pMalloc->Release();
			}
			else
			{
				ErrorLog("createNewShellScanProcess ERROR SHGetMalloc");
			}
			iFolder->Release();
		}
		else
		{
			ErrorLog("createNewShellScanProcess ERROR SHGetDesktopFolder");
		}
	}
	else
	{
		ErrorLog("createNewShellScanProcess ERROR CLSIDFromString");
		ErrorLog(szwAvCLSID,lstrlenW(szwAvCLSID)*2);
	}


	delete cdobj;
	return result;

}



jint JNICALL Java_CheckManager_JNI_createDummySpi(JNIEnv *env, jclass jobj,
												  jstring avExeName,
												jstring MainWinTitle,
												jstring MainWinClass)
{
	char tmpBuf[250];
	jboolean isCopy=false;

	WCHAR *sFileBuf =(WCHAR*)env->GetStringChars(avExeName,&isCopy);
	WideCharToMultiByte(CP_ACP,0,(LPCWSTR)sFileBuf,-1,tmpBuf,250,0,0);
	env->ReleaseStringChars(avExeName,(const jchar *)sFileBuf);

	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO *)GlobalAlloc(GPTR,sizeof(SCAN_PROCESS_INFO));
	CharLowerBuff(tmpBuf,lstrlen(tmpBuf));
	spi->avExeNameHash = CalcHash(tmpBuf);

	WCHAR *szwMainWinTitle =(WCHAR *)env->GetStringChars(MainWinTitle,0);
	if(lstrlenW(szwMainWinTitle)>1)
	{
		spi->szMainWinTitle = WCharToChar(szwMainWinTitle);
	}
	env->ReleaseStringChars(MainWinTitle,(const jchar *)szwMainWinTitle);

	WCHAR *szwMainWinClass =(WCHAR *)env->GetStringChars(MainWinClass,0);
	if(lstrlenW(szwMainWinClass)>1)
	{
		spi->szMainWinClass = WCharToChar(szwMainWinClass);
	}
	env->ReleaseStringChars(MainWinClass,(const jchar *)szwMainWinClass);

	return (int)spi;
}


jstring JNICALL Java_CheckManager_JNI_tray_GetTooltipText(JNIEnv *env, jclass jobj,jstring ttPattern,jstring fileName)
{
	char szPattern[250];
	char szFileName[250];
	jboolean isCopy=false;
	
	RtlZeroMemory(szFileName,250);
	WCHAR *wttPattern =(WCHAR*)env->GetStringChars(ttPattern,&isCopy);
	WideCharToMultiByte(CP_ACP,0,(LPCWSTR)wttPattern,-1,szPattern,250,0,0);
	env->ReleaseStringChars(ttPattern,(const jchar *)wttPattern);

	WCHAR *wszFileName =(WCHAR*)env->GetStringChars(fileName,&isCopy);
	WideCharToMultiByte(CP_ACP,0,(LPCWSTR)wszFileName,-1,szFileName,250,0,0);
	env->ReleaseStringChars(fileName,(const jchar *)wszFileName);

	CTray cTray;
	char *szTTip = cTray.getTooltipText(szPattern,szFileName);
	WCHAR *uStr = L"";

	if(szTTip)
	{
		uStr = CharToWChar(szTTip);
		GlobalFree(szTTip);
	}

	jstring retData = env->NewString((jchar *)uStr,lstrlenW(uStr));
	GlobalFree(uStr);
	return retData;
}


jboolean JNICALL Java_CheckManager_JNI_tray_TrackPopupMenu(JNIEnv *env, jclass jobj,jstring ttPattern,jstring fileName)
{
	char szPattern[250];
	char szFileName[250];
	jboolean isCopy=false;
	
	RtlZeroMemory(szFileName,250);
	WCHAR *wttPattern =(WCHAR*)env->GetStringChars(ttPattern,&isCopy);
	WideCharToMultiByte(CP_ACP,0,(LPCWSTR)wttPattern,-1,szPattern,250,0,0);
	env->ReleaseStringChars(ttPattern,(const jchar *)wttPattern);

	WCHAR *wszFileName =(WCHAR*)env->GetStringChars(fileName,&isCopy);
	WideCharToMultiByte(CP_ACP,0,(LPCWSTR)wszFileName,-1,szFileName,250,0,0);
	env->ReleaseStringChars(fileName,(const jchar *)wszFileName);

	CTray cTray;
	return (jboolean)cTray.trackPopupMenu(szPattern,szFileName);
}



jboolean JNICALL Java_CheckManager_JNI_tray_DblClick(JNIEnv *env, jclass jobj,jstring ttPattern,jstring fileName)
{
	char szPattern[250];
	char szFileName[250];
	jboolean isCopy=false;
	
	RtlZeroMemory(szFileName,250);
	WCHAR *wttPattern =(WCHAR*)env->GetStringChars(ttPattern,&isCopy);
	WideCharToMultiByte(CP_ACP,0,(LPCWSTR)wttPattern,-1,szPattern,250,0,0);
	env->ReleaseStringChars(ttPattern,(const jchar *)wttPattern);

	WCHAR *wszFileName =(WCHAR*)env->GetStringChars(fileName,&isCopy);
	WideCharToMultiByte(CP_ACP,0,(LPCWSTR)wszFileName,-1,szFileName,250,0,0);
	env->ReleaseStringChars(fileName,(const jchar *)wszFileName);

	CTray cTray;
	jboolean result = (jboolean)cTray.DblClick(szPattern,szFileName);

	return result;
}



jint JNICALL Java_CheckManager_JNI_createNewShellScanProcess(JNIEnv *env , 
												jclass jobj, 
												jstring avCLSID,
												jstring checkFilePath,
												jint menuID,
												jstring avExeName,
												jstring MainWinTitle,
												jstring MainWinClass)
{
	BOOL					result = false;
	
	
	

	DebugLog("JNI_createNewShellScanProcess");

	CoInitialize(0);
	WCHAR *szwAvCLSID =(WCHAR*)env->GetStringChars(avCLSID,0);
	WCHAR *szwCheckFilePath =(WCHAR *)env->GetStringChars(checkFilePath,0);
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO *)GlobalAlloc(GPTR,sizeof(SCAN_PROCESS_INFO));
	WCHAR *szwMainWinTitle =(WCHAR *)env->GetStringChars(MainWinTitle,0);
	if(lstrlenW(szwMainWinTitle)>1)
	{
		spi->szMainWinTitle = WCharToChar(szwMainWinTitle);
	}
	env->ReleaseStringChars(MainWinTitle,(const jchar *)szwMainWinTitle);

	WCHAR *szwMainWinClass =(WCHAR *)env->GetStringChars(MainWinClass,0);
	if(lstrlenW(szwMainWinClass)>1)
	{
		spi->szMainWinClass = WCharToChar(szwMainWinClass);
	}
	env->ReleaseStringChars(MainWinClass,(const jchar *)szwMainWinClass);

	WCHAR *szwAvExeName =(WCHAR *)env->GetStringChars(avExeName,0);
	char *szAvExeName = WCharToChar(szwAvExeName);
	CharLowerBuff(szAvExeName,lstrlen(szAvExeName));
	spi->avExeNameHash = CalcHash(szAvExeName);
	GlobalFree(szAvExeName);
    env->ReleaseStringChars(avExeName,(const jchar *)szwAvExeName);


	__try
	{
		result = createNewShellScanProcess(szwAvCLSID,szwCheckFilePath,menuID,spi);
	
	}
	__except(EXCEPTION_CONTINUE_EXECUTION)
	{
		DebugLog("JNI_createNewShellScanProcess:: exception");
	}
	
	env->ReleaseStringChars(avCLSID,(const jchar *)szwAvCLSID);
	env->ReleaseStringChars(checkFilePath,(const jchar *)szwCheckFilePath);
	CoUninitialize();

	if(!result)
	{
		if(spi->szMainWinTitle) GlobalFree(spi->szMainWinTitle);
		if(spi->szMainWinClass) GlobalFree(spi->szMainWinClass);
		GlobalFree(spi);
		return 0;
	}
		
	return (int)spi;
}


jboolean JNICALL Java_CheckManager_JNI_isProcessFinished(JNIEnv *env, jclass jobj, jint pi)
{
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)pi;
	return (WaitForSingleObject(spi->pi.hProcess,0) == WAIT_OBJECT_0);
}


jboolean JNICALL Java_CheckManager_JNI_writeProcessOutputData(JNIEnv *env, jclass jobj, jint pi, jstring outFileName)
{
	char tmpBuf[512];
	DWORD nBytesRead,bw;
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)pi;
	bool rez = false;

	WCHAR *szOutFile =(WCHAR *)env->GetStringChars(outFileName,0);
	HANDLE hOutFile = CreateFileW(szOutFile,GENERIC_WRITE,FILE_SHARE_READ,0,OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL,0);
	env->ReleaseStringChars(outFileName,(const jchar *)szOutFile);
	if(hOutFile)
	{
		while(TRUE)
		{
			if(!ReadFile(spi->hOutputRead,tmpBuf,sizeof(tmpBuf),&nBytesRead,NULL) || nBytesRead == 0)
			{
				if(GetLastError() == ERROR_BROKEN_PIPE)	
					break; 
			}
			else
			{
				rez = true;
				WriteFile(hOutFile,tmpBuf,nBytesRead,&bw,0);
			}
		}

		CloseHandle(hOutFile);
	}

	return rez;
}


void JNICALL Java_CheckManager_JNI_freePROCESSStruct(JNIEnv *env, jclass jobj, jint pi)
{
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)pi;
	
	if(spi)
	{
		if(spi->pi.hProcess)
		{
			if(WaitForSingleObject(spi->pi.hProcess,0) == WAIT_TIMEOUT)
			{
				TerminateProcess(spi->pi.hProcess,0);
			}
		}
		
		if(spi->hOutputWrite)	CloseHandle(spi->hOutputWrite);
		if(spi->hInputRead)		CloseHandle(spi->hInputRead);
		if(spi->hErrorWrite)	CloseHandle(spi->hErrorWrite);
		if(spi->hOutputRead)	CloseHandle(spi->hOutputRead);
		if(spi->hInputWrite)	CloseHandle(spi->hInputWrite);
		if(spi->pi.hProcess)	CloseHandle(spi->pi.hProcess);
		if(spi->szMainWinClass)	GlobalFree(spi->szMainWinClass);
		if(spi->szMainWinTitle)	GlobalFree(spi->szMainWinTitle);
		GlobalFree(spi);
	}
}


jboolean JNICALL Java_CheckManager_JNI_clickByString(JNIEnv *env, 
									  jclass jobj, 
									  jstring elemName,
									  jint pi)
{
	OutputDebugString("JNI_clickByString");

	jboolean result = false;
	WCHAR *szwElemName =(WCHAR *)env->GetStringChars(elemName,0);
	char *szElemName = WCharToChar(szwElemName);
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)pi;

	env->ReleaseStringChars(elemName,(const jchar *)szwElemName);

	char *szResponce;
	if(IpcSendMessage("getStringXY",&szResponce,spi,szElemName) > 0)
	{
		if(spi->rootHwnd == 0)
		{
			EnumWindows(EnumGetRootHwndWindowsProc,(LPARAM)spi);
		}
		
		POINT pt;
		
		OutputDebugString(szResponce);

		char szTMpBuf[512];
		wsprintf(szTMpBuf,"spi->szMainWinClas=%s sspi->rootHwnd = 0x%X",spi->szMainWinClass,spi->rootHwnd);
		OutputDebugString(szTMpBuf);

		char *pStr = StrChrI(szResponce,'x');
		pStr[0] = 0;
		pStr++;
		pt.x = StrToInt(szResponce);
		pt.y = StrToInt(pStr);

		ClientToScreen(spi->rootHwnd,&pt);

		BringWindowToTop(spi->rootHwnd);
		SetForegroundWindow(spi->rootHwnd);
		SetCursorPos(pt.x,pt.y);
		mouse_event(MOUSEEVENTF_LEFTDOWN | MOUSEEVENTF_LEFTUP,pt.x,pt.y,0,0);
		result = true;
	}

	GlobalFree(szElemName);
	return result;
}



jboolean JNICALL Java_CheckManager_JNI_sendMessage(JNIEnv *env, 
									  jclass jobj, 
									  jstring elemName,
									  jstring className,
									  jint elemID,
									  jint msg,
									  jint wParam,
									  jint lParam,
									  jint pi)
{
	jboolean result = false;

	SEND_MSG_PARAM mp;
	RtlZeroMemory(&mp,sizeof(mp));

	WCHAR *szwClassName=0;
	WCHAR *szwElemName =(WCHAR *)env->GetStringChars(elemName,0);
	if(lstrlenW(szwElemName) > 0)
	{
		mp.elemName = WCharToChar(szwElemName);
	}
	env->ReleaseStringChars(elemName,(const jchar *)szwElemName);
	
	if(!mp.elemName)
	{
		szwClassName =(WCHAR *)env->GetStringChars(className,0);
		if(lstrlenW(szwClassName) > 0)
		{
			mp.className = WCharToChar(szwClassName);
		}
		env->ReleaseStringChars(className,(const jchar *)szwClassName);
	}

	mp.spi = (SCAN_PROCESS_INFO*)pi;
	mp.elemID = elemID;
	mp.lParam = lParam;
	mp.msg = msg;
	mp.wParam = wParam;
	
	if(mp.spi)
	{
		mp.Result = false;

			EnumWindows(sendMsgEnumParentWindowsProc,(LPARAM)&mp);


		result = mp.Result;
	}

	if(mp.elemName) GlobalFree(mp.elemName);
	if(mp.className) GlobalFree(mp.className);

	return result;
}



void JNICALL Java_CheckManager_JNI_terminateProcess(JNIEnv *env, jclass jobj,jint pi)
{
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)pi;
	if(spi && spi->pi.hProcess)
		TerminateProcess(spi->pi.hProcess,0);
}


void JNICALL Java_CheckManager_JNI_InitEngine(JNIEnv *env, jclass jobj)
{
	DebugLog("Java_CheckManager_JNI_InitEngine");
	GetModuleFileName(hMod,szModulePath,512);
	PathRemoveFileSpec(szModulePath);
	RtlMoveMemory(szAvConfig,szModulePath,lstrlen(szModulePath));
	lstrcat(szAvConfig,"\\avscan");
	pRtlTimeToSecondsSince1970 = (Ptr_RtlTimeToSecondsSince1970)GetProcAddress(LoadLibrary("ntdll.dll"),"RtlTimeToSecondsSince1970");
	cpuUsage = new CCpuUsage();
	cpuUsage->GetCpuUsage();
}


IXMLDOMDocument2* pHookXmlDoc = 0;
BOOL stopFlag = false;

typedef struct HOOK_INFO_PIPE
{
	char *szPipeName;
	WCHAR *xmlInfo;
	int xmlInfoSize;
};


DWORD WINAPI HookInfoPipeServerThread(LPVOID lpParameter)
{
	DWORD bw;
	HOOK_INFO_PIPE *hip = (HOOK_INFO_PIPE*)lpParameter;
	
	DebugLog("HookInfoPipeServerThread");
	DebugLog(hip->szPipeName);

	while(!stopFlag)
	{
		HANDLE hPipe = CreateNamedPipe(hip->szPipeName,PIPE_ACCESS_DUPLEX,PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT,
								PIPE_UNLIMITED_INSTANCES,
								1024,
								1024,0,NULL);

		if(ConnectNamedPipe(hPipe, NULL))
		{
			int index=0;
			while(index<hip->xmlInfoSize)
			{
				if(!WriteFile(hPipe,hip->xmlInfo+index,hip->xmlInfoSize-index,&bw,0)) break;
				index+=bw;
			}
				
			FlushFileBuffers(hPipe); 
			DisconnectNamedPipe(hPipe);
		}

		CloseHandle(hPipe);
	}

	GlobalFree(hip->szPipeName);
	GlobalFree(hip->xmlInfo);
	GlobalFree(hip);
	return 0;
}



#define IOCTL_SET_HOOK_NAMES CTL_CODE(FILE_DEVICE_UNKNOWN,0x801,METHOD_BUFFERED,FILE_ANY_ACCESS)
#define IOCTL_SET_INJECT_DLL_PATH CTL_CODE(FILE_DEVICE_UNKNOWN,0x802,METHOD_BUFFERED,FILE_ANY_ACCESS)

BOOL deleteService(char *szServiceName)
{
	BOOL result = false;

	SC_HANDLE scHandle = OpenSCManager(NULL,NULL,SC_MANAGER_ALL_ACCESS);
	if(!scHandle)
	{

	}
	else
	{
		SC_HANDLE Service = OpenService(scHandle,szServiceName,SERVICE_ALL_ACCESS);
		if(!Service)
		{

		}
		else
		{
			result = DeleteService(Service);
			CloseServiceHandle(Service);
		}

		CloseServiceHandle(scHandle);
	}

	return result;
}

BOOL startService(char *szServiceName)
{
	BOOL result = false;

	SC_HANDLE scHandle = OpenSCManager(NULL,NULL,SC_MANAGER_ALL_ACCESS);
	if(!scHandle)
	{
		//writeLogFile("ERROR OpenSCManager");
	}
	else
	{
		SC_HANDLE Service = OpenService(scHandle,szServiceName,SERVICE_ALL_ACCESS);
		if(!Service)
		{
			//writeLogFile("ERROR OpenService");
		}
		else
		{
			result = StartService(Service,0,0);
			CloseServiceHandle(Service);
		}

		CloseServiceHandle(scHandle);
	}

	return result;
}

BOOL stopService(char *szServiceName)
{
	BOOL result = false;

	SC_HANDLE scHandle = OpenSCManager(NULL,NULL,SC_MANAGER_ALL_ACCESS);
	if(!scHandle)
	{
		//writeLogFile("ERROR OpenSCManager");
	}
	else
	{
		SC_HANDLE Service = OpenService(scHandle,szServiceName,SERVICE_ALL_ACCESS);
		if(!Service)
		{
			//writeLogFile("ERROR OpenService");
		}
		else
		{
			SERVICE_STATUS ss;
			result = ControlService(Service,SERVICE_CONTROL_STOP,&ss);
			CloseServiceHandle(Service);
		}

		CloseServiceHandle(scHandle);
	}

	return result;
}


BOOL createService(char *szServiceName,char* szDescription,char *szFilePath)
{
	BOOL result = false;

	SC_HANDLE scHandle = OpenSCManager(NULL,NULL,SC_MANAGER_ALL_ACCESS);
	if(!scHandle)
	{
		//writeLogFile("ERROR OpenSCManager");
	}
	else
	{
		SC_HANDLE Service = CreateService(scHandle,
												szServiceName,
												szDescription,
												SERVICE_ALL_ACCESS,
												SERVICE_KERNEL_DRIVER,
												SERVICE_DEMAND_START,
												SERVICE_ERROR_NORMAL,
												szFilePath,
												0,0,0,0,0);
		if(!Service)
		{
			//writeLogFile("ERROR CreateService");
		}
		else
		{
			result = true;
			CloseServiceHandle(Service);
		}

		CloseServiceHandle(scHandle);
	}

	return result;
}

void JNICALL Java_CheckManager_JNI_StopHookEngine(JNIEnv *env, jclass jobj)
{
	stopService("VF");
	deleteService("VF");

}




void JNICALL Java_CheckManager_JNI_InitHookEngine(JNIEnv *env, jclass jobj,jstring HookXmlConfig)
{
	char tmpBuf[512];
	char *szInjectToArr = 0;
	
	pHookXmlDoc = xmlInit();
	if(pHookXmlDoc)
	{
		WCHAR *wzHookXmlConfig =(WCHAR *)env->GetStringChars(HookXmlConfig,0);
		DebugLog(wzHookXmlConfig,lstrlenW(wzHookXmlConfig)*2);

		if(xmlLoad(wzHookXmlConfig,pHookXmlDoc))
		{

			IXMLDOMNodeList *itemList;
			if(xmlGetPathedNodeList2(pHookXmlDoc,"//item",&itemList))
			{
				long listSize=0;
				IXMLDOMNode *node,*injectToNode,*hookInfoNode;
				itemList->get_length(&listSize);
				
				for(int i=0; i<listSize;i++)
				{
					itemList->get_item(i,&node);
					HOOK_INFO_PIPE *hip = (HOOK_INFO_PIPE*)GlobalAlloc(GPTR,sizeof(HOOK_INFO_PIPE));
					
					if(xmlGetPathedChildNode(node,"injectTo",&injectToNode))
					{
						char *szinjectTo = xmlGetNodeValue(injectToNode);
						szInjectToArr = (char*)MyRealloc(szInjectToArr,lstrlen(szinjectTo)+2);
						lstrcat(szInjectToArr,szinjectTo);
						lstrcat(szInjectToArr,"\r\n");
						
						wsprintf(tmpBuf,"\\\\.\\pipe\\%s_hip",szinjectTo);
						hip->szPipeName = (char*)GlobalAlloc(GPTR,lstrlen(tmpBuf)+1);
						lstrcat(hip->szPipeName,tmpBuf);
						
						GlobalFree(szinjectTo);

						if(xmlGetPathedChildNode(node,"hook_functions",&hookInfoNode))
						{
							hookInfoNode->get_xml(&hip->xmlInfo);
							hip->xmlInfoSize = lstrlenW(hip->xmlInfo)*sizeof(WCHAR);
							hookInfoNode->Release();
						}
						injectToNode->Release();

						DWORD tid;
						CloseHandle(CreateThread(0,0,HookInfoPipeServerThread,hip,0,&tid));
					}

					node->Release();
				}
				
				itemList->Release();
			}
		}

		xmlFree(pHookXmlDoc);
		env->ReleaseStringChars(HookXmlConfig,(const jchar *)wzHookXmlConfig);
	
		GetModuleFileName(hMod,tmpBuf,512);
		char *pFileName = PathFindFileName(tmpBuf);
		RtlMoveMemory(pFileName,"inject.sys",11);
		stopService("VF");
		deleteService("VF");
		if(createService("VF","VF",tmpBuf))
		{
		}
			if(startService("VF"))
			{
				HANDLE hDriver = CreateFile("\\\\.\\VF",
												GENERIC_READ | GENERIC_WRITE,
												FILE_SHARE_READ|FILE_SHARE_WRITE,
												NULL,
												OPEN_EXISTING,
												FILE_ATTRIBUTE_NORMAL,
												NULL);
				if(hDriver != INVALID_HANDLE_VALUE)
				{
					DWORD br;
					GetModuleFileName(hMod,tmpBuf,512);
					PathRemoveFileSpec(tmpBuf);
					lstrcat(tmpBuf,"\\hook_dll.dll");
						
					if(!DeviceIoControl(hDriver,IOCTL_SET_INJECT_DLL_PATH,tmpBuf,lstrlen(tmpBuf),NULL,NULL,&br,NULL))
					{
					
					}
					
					if(!DeviceIoControl(hDriver,IOCTL_SET_HOOK_NAMES,szInjectToArr,lstrlen(szInjectToArr),NULL,NULL,&br,NULL))
					{
						
					}

					CloseHandle(hDriver);
				}
				else
				{
					ErrorLog("ERROR CreateFile");
				}
			}
			else
			{
				ErrorLog("ERROR startService");
				wsprintf(tmpBuf,"%0X",GetLastError());
				ErrorLog(tmpBuf);
			}


		InvalidateRect(0,0,TRUE);

		
	}
}
	

char * getJavaStringParam(JNIEnv *env,jstring paramString)
{
	char *szParam = 0;
	WCHAR *szwParam =(WCHAR *)env->GetStringChars(paramString,0);
	if(szwParam)
	{
		if(lstrlenW(szwParam) > 0)
		{
			szParam = WCharToChar(szwParam);
		}
	}
	env->ReleaseStringChars(paramString,(const jchar *)szwParam);
	return szParam;
}


jboolean JNICALL Java_CheckManager_JNI_isShellAvExist(JNIEnv *env, jclass jobj,jstring avCLSID)
{
	WCHAR *szwAvCLSID =(WCHAR*)env->GetStringChars(avCLSID,0);
	CLSID pclsid;
	boolean result = false;
	IShellExtInit			*ishel;

	if(CLSIDFromString(szwAvCLSID,&pclsid) == S_OK)
	{
		HRESULT hr=CoCreateInstance(pclsid,NULL,CLSCTX_INPROC_SERVER | CLSCTX_LOCAL_SERVER | CLSCTX_INPROC_HANDLER,
									IID_IShellExtInit,(void**)&ishel);
		if(hr == S_OK)
		{
			result = true;
			ishel->Release();
		}
	}

	env->ReleaseStringChars(avCLSID,(const jchar *)szwAvCLSID);

	return result;
}


jstring JNICALL Java_CheckManager_JNI_getClipboardText(JNIEnv *env, jclass jobj)
{
	WCHAR *uStr = 0;

	if(IsClipboardFormatAvailable(CF_TEXT)) 
	{
		if(OpenClipboard(hwndMain))
		{
            HANDLE hClipboard = GetClipboardData(CF_TEXT); 
			if(hClipboard != NULL) 
			{ 
				LPTSTR lptstr = GlobalLock(hClipboard); 
				if(lptstr != NULL) 
				{
					uStr = CharToWChar(lptstr);
					GlobalUnlock(hClipboard);
				}
			}
		}
	}

	jstring retData = env->NewString((jchar *)uStr,lstrlenW(uStr));
	if(uStr != 0) GlobalFree(uStr);
	return retData;
}


jstring JNICALL Java_CheckManager_JNI_tray_GetMenuText(JNIEnv *env, jclass jobj,jstring clickType,jint count,jstring fileName,jstring tooltipRegEx)
{
	WCHAR *retData = 0;
	char *szClickType = getJavaStringParam(env,clickType);	// lbtn-rbtn
	char *szFileName = getJavaStringParam(env,fileName);
	char *szToolTipRegEx = getJavaStringParam(env,tooltipRegEx);

	GetMenu(

	if(szClickType) GlobalFree(szClickType);
	if(szFileName) GlobalFree(szFileName);
	if(szToolTipRegEx) GlobalFree(szToolTipRegEx);

	return retData;
}

PROCESSENTRY32 *findProcessByName(char *pName)
{
	BOOLEAN isFound = FALSE;
	PROCESSENTRY32 *pe32 = GlobalAlloc(GPTR,sizeof(PROCESSENTRY32));
	HANDLE hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
	pe32->dwSize = sizeof( PROCESSENTRY32 );
	if(Process32First( hProcessSnap, pe32 ))
	{										
		do
		{
			if(StrCmpI(pe32->szExeFile,pName) == 0)
			{
				isFound = TRUE;	
				break;
			}
		} while( Process32Next( hProcessSnap, pe32 ) );
	}

	CloseHandle( hProcessSnap );
	if(isFound == FALSE)
	{
		GlobalFree(pe32);
		pe32 = 0;
	}

	return pe32;
}


struct WIN_INFO
{
	DWORD pid;
	char *szTitle;
	HWND hWnd;

};

BOOL CALLBACK FindWinTitleProc(HWND hwnd,LPARAM lParam)
{
	DWORD lngProcessID;
	WIN_INFO *wi = (WIN_INFO*)lParam;
	DWORD result;
	char szWndInfoBuf[500];
	
	GetWindowThreadProcessId(hwnd,&lngProcessID);
	if(wi->pid == 0 || lngProcessID == wi->pid)
	{
		szWndInfoBuf[0] = 0;
		GetWindowText(hwnd,szWndInfoBuf,500);
		if(wi->szTitle == 0 || StrStrI(szWndInfoBuf,wi->szTitle))
		{
			wi->hWnd = hwnd;
			return false;
		}

		EnumChildWindows(hwnd,FindWinTitleProc,lParam);
		if(wi->hWnd) return false;
	}

	return true;
}



jstring JNICALL Java_CheckManager_JNI_getWinTitle(JNIEnv *env, jclass jobj,jstring fileName,jstring winTitle)
{
	WIN_INFO wi;
	char szWndInfoBuf[500];
	char *szfileName = getJavaStringParam(env,fileName);
	wi.szTitle = getJavaStringParam(env,winTitle);
	wi.hWnd = 0;
	wi.pid = 0;
	szWndInfoBuf[0] = 0;
	
	if(szfileName)
	{
		PROCESSENTRY32 *pe32 = findProcessByName(szfileName);
		if(pe32)
		{
			wi.pid = pe32->th32ProcessID;
			GlobalFree(pe32);
		}
	}
	
	if(wi.szTitle || wi.pid)
	{
		EnumWindows(FindWinTitleProc,&wi);
		if(wi.hWnd)
		{
			GetWindowText(wi.hWnd,szWndInfoBuf,500);
		}
	}
	
	if(szfileName) GlobalFree(szfileName);
	if(wi.szTitle) GlobalFree(wi.szTitle);

	WCHAR *uStr = CharToWChar(szWndInfoBuf);
	jstring retData = env->NewString((jchar *)uStr,lstrlenW(uStr));
	GlobalFree(uStr);
	
	return retData;
}


jboolean JNICALL Java_CheckManager_JNI_isWinExist(JNIEnv *env, jclass jobj,jstring fileName,jstring winTitle)
{
	WIN_INFO wi;
	jboolean result = false;
	char *szfileName = getJavaStringParam(env,fileName);
	wi.szTitle = getJavaStringParam(env,winTitle);
	wi.hWnd = 0;
	wi.pid = 0;

	if(szfileName)
	{
		PROCESSENTRY32 *pe32 = findProcessByName(szfileName);
		if(pe32)
		{
			wi.pid = pe32->th32ProcessID;
			GlobalFree(pe32);
		}
	}
	
	if(wi.szTitle || wi.pid)
	{
		EnumWindows(FindWinTitleProc,&wi);
		if(wi.hWnd)
		{
			result = true;
		}
	}
	
	if(szfileName) GlobalFree(szfileName);
	if(wi.szTitle) GlobalFree(wi.szTitle);

	return result;
}

jboolean JNICALL Java_CheckManager_JNI_menuClick(JNIEnv *env, jclass jobj,jstring fileName,jstring itemPath,jstring type,jstring winTitle)
{

}

jstring JNICALL Java_CheckManager_JNI_regGetValue(JNIEnv *env, jclass jobj,jstring key,jstring subKey,jstring valueName)
{
	char *szRegKey = getJavaStringParam(env,key);
	char *szRegSubKey = getJavaStringParam(env,subKey);
	char *szRegValueName = getJavaStringParam(env,valueName);
	HKEY hkey;
	HKEY hpKey;
	DWORD tmp,retDataSize;
	char *szRetData="";
	
	if(StrCmpI(szRegKey,"HCR") == 0) hkey = HKEY_CLASSES_ROOT;
	else if(StrCmpI(szRegKey,"HCU") == 0) hkey = HKEY_CURRENT_USER;
	else if(StrCmpI(szRegKey,"HLM") == 0) hkey = HKEY_LOCAL_MACHINE;
	else if(StrCmpI(szRegKey,"HU") == 0) hkey = HKEY_USERS;

	if(RegOpenKeyEx(hkey,subKey,0,0,&hpKey) == ERROR_SUCCESS)
	{
		tmp = REG_SZ;
		retDataSize = 0;
		if(RegQueryValueEx(hpKey,szRegValueName,0,&tmp,0,&retDataSize) == ERROR_SUCCESS)
		{
			retData = GlobalAlloc(GPTR,retDataSize+1);
			RegQueryValueEx(hpKey,szRegValueName,0,&tmp,szRetData,&retDataSize);
		}

		RegCloseKey(phKey);
	}

	if(szRegKey) GlobalFree(szRegKey);
	if(szRegPath) GlobalFree(szRegPath);
	if(szRegValueName) GlobalFree(szRegValueName);

	WCHAR *uStr = CharToWChar(szRetData);
	jstring retData = env->NewString((jchar *)uStr,lstrlenW(uStr));
	GlobalFree(uStr);

	return retData;
}

jstring JNICALL Java_CheckManager_JNI_menuGetItemState(JNIEnv *env, jclass jobj,jstring fileName,jstring itemPath,jstring winTitle)
{

}

jstring JNICALL Java_CheckManager_JNI_getProcessOutputData(JNIEnv *env, jclass jobj,jint pi)
{

}

	

	
