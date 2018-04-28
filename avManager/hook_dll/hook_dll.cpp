
#include "hook_dll.h"
#include "Array.h"


HMODULE			hMod;
BOOL			funkHooked;
HANDLE			hHeap;

pTextOutW p_TextOutWProc;
pTextOutW p_TextOutWOldProc;

pDrawTextW p_DrawTextWProc;
pDrawTextW p_DrawTextWOldProc;

pDrawTextExW p_DrawTextExWProc;
pDrawTextExW p_DrawTextExWOldProc;

pExtTextOutW p_ExtTextOutWProc;
pExtTextOutW p_ExtTextOutWOldProc;

pExtTextOutA p_ExtTextOutAProc;
pExtTextOutA p_ExtTextOutAOldProc;

pGetTextExtentPointW p_GetTextExtentPointWProc;
pGetTextExtentPointW p_GetTextExtentPointWOldProc;

myArray wordsArr;
myArray wordsInfoArr;

myArraySort wordsArraySort;


CRITICAL_SECTION grabDataSection;
char *szTextData = 0;
BOOL stopFlag=false;

BOOL  hashstr=true;
BOOL compiletostringline=false;

#define PIPE_BY_PROCESS_ID 0
#define PIPE_BY_EXE_NAME 1


BOOL APIENTRY DllMain(HMODULE hModule,DWORD fdwReason,LPVOID lpReserved)
{
	switch(fdwReason)
	{	
		case DLL_PROCESS_DETACH:
		{
			stopFlag = true;
			OutputDebugString("HOOK_DLL: DLL_PROCESS_DETACH");
			delFuncHooks();
			break;
		}

		case DLL_PROCESS_ATTACH:
		{

			char lpFileName[512];
			OutputDebugString("HOOK_DLL: DLL_PROCESS_ATTACH");
			GetModuleFileName(hModule,lpFileName,512);
            OutputDebugString(lpFileName);

			hMod = hModule;
			CoInitialize(0);
			InitializeCriticalSection(&grabDataSection);
			hHeap = GetProcessHeap();

			DWORD tid;
			CloseHandle(CreateThread(0,0,&HookInfoThread,0,0,&tid));
			CloseHandle(CreateThread(0,0,&CommandListenerThread,PIPE_BY_PROCESS_ID,0,&tid));
			CloseHandle(CreateThread(0,0,&CommandListenerThread,(LPVOID)PIPE_BY_EXE_NAME,0,&tid));



			 break;
		}

	}
    return TRUE;
}



DWORD WINAPI HookInfoThread(LPVOID lpParameter)
{
	char tmpBuf[MAX_PATH];
	DWORD bw;
	DWORD cbBytesRead;
	char *chRequest = (char*)GlobalAlloc(GPTR,4096);

	GetModuleFileName(0,tmpBuf,MAX_PATH);
	char *szFileNamePtr = PathFindFileName(tmpBuf);
	wsprintf(tmpBuf,"\\\\.\\pipe\\%s_hip",szFileNamePtr);

	writeLogFile("HookInfoThread");
	writeLogFile(tmpBuf);
	BOOL exitFlag = FALSE;

	while(exitFlag == false && stopFlag == false)
	{
		HANDLE hPipe = CreateFile(tmpBuf,GENERIC_READ | GENERIC_WRITE ,0,0,OPEN_EXISTING,0,0);
		if(hPipe != INVALID_HANDLE_VALUE)
		{
			DWORD dwMode = PIPE_READMODE_BYTE; 
			BOOL fSuccess = SetNamedPipeHandleState(hPipe,&dwMode,NULL,NULL);
		
			int RespSize=0;
			WCHAR* szResponce = (WCHAR*)MyRealloc(0,1024);
			while(ReadFile(hPipe,LPVOID(szResponce+RespSize),1024,&bw,0))				
			{
				RespSize += bw;
				szResponce = (WCHAR*)MyRealloc(szResponce,1024);
			}
			
			if(RespSize == 0)
			{
				MyFree(szResponce);
			}
			else
			{
				int xmlConfigSize = RespSize;
				WCHAR *xmlConfig = (WCHAR*)GlobalAlloc(GPTR,xmlConfigSize+14*2);
				RtlMoveMemory(xmlConfig,L"<root>",6*2);
				RtlMoveMemory(xmlConfig+6,szResponce,RespSize);
				RtlMoveMemory(xmlConfig+6+RespSize/2,L"</root>",7*2);
				MyFree(szResponce);

				IXMLDOMDocument2* pHookXmlDoc = 0;
				pHookXmlDoc = xmlInit();
				if(pHookXmlDoc)
				{
					if(xmlLoad(xmlConfig,pHookXmlDoc))
					{
						IXMLDOMNodeList *itemList;

						OutputDebugString("HookInfoThread");
						OutputDebugString(WCharToChar(xmlConfig));

						IXMLDOMNode *node = xmlGetNode(pHookXmlDoc,"//hook_options");
						if(node)
						{
							char *szTmp = xmlGetNodeAttribute(node,"hashstr");
							if(szTmp)
							{

								if(szTmp[0] == 'n') hashstr = false;
								GlobalFree(szTmp);
							}

							szTmp = xmlGetNodeAttribute(node,"compiletostringline");
							if(szTmp)
							{

								if(szTmp[0] == 'y') compiletostringline = true;
								GlobalFree(szTmp);
							}

							node->Release();
						}

						
						if(xmlGetPathedNodeList2(pHookXmlDoc,"//func",&itemList))
						{
							IXMLDOMNode *node;
							while(!itemList->nextNode(&node))
							{
								char *szFuncName = xmlGetNodeValue(node);
								setFuncHooks(szFuncName);
								GlobalFree(szFuncName);
								node->Release();
							}

							itemList->Release();
							InvalidateRect(0,0,TRUE);
							exitFlag = TRUE;
						}
						else
						{
							writeLogFile("ERROR xmlGetPathedNodeList2");
						}
					}
					else
					{
						writeLogFile("ERROR xmlLoad");
					}

					xmlFree(pHookXmlDoc);
				}
				else
				{
					writeLogFile("ERROR xmlInit");
				}
				
				GlobalFree(xmlConfig);
				
			}


		
			CloseHandle(hPipe);
		
		}
		else
		{
			writeLogFile("HookInfoThread::CreateFile ERROR");
			writeLogFile(tmpBuf);
		}

		Sleep(10);
	}

	return 0;
}




DWORD WINAPI CommandListenerThread(LPVOID lpParameter)
{
	char tmpBuf[512];
	DWORD bw;
	DWORD cbBytesRead;
	char *chRequest = (char*)GlobalAlloc(GPTR,4096);
	
	if(lpParameter == PIPE_BY_PROCESS_ID)
	{
		wsprintf(tmpBuf,"\\\\.\\pipe\\%u",GetCurrentProcessId());
	}
	else
	{
		GetModuleFileName(0,tmpBuf,512);
		OutputDebugString(tmpBuf);
		char *szFileNamePtr = PathFindFileName(tmpBuf);
		CharLowerBuff(szFileNamePtr,lstrlen(szFileNamePtr));
		OutputDebugString(szFileNamePtr);
		wsprintf(tmpBuf,"\\\\.\\pipe\\%u",CalcHash(szFileNamePtr));
	}

	OutputDebugString("CommandListenerThread");
	OutputDebugString(tmpBuf);
	writeLogFile("CommandListenerThread");
	writeLogFile(tmpBuf);
	
	while(!stopFlag)
	{
		HANDLE hPipe = CreateNamedPipe(tmpBuf,
			PIPE_ACCESS_DUPLEX,PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT,
								PIPE_UNLIMITED_INSTANCES,
								1024,
								1024,0,NULL);

		if(ConnectNamedPipe(hPipe, NULL))
		{
			DWORD totalRead = 0;
			RtlZeroMemory(chRequest,4096);
			for(;;)
			{
				if(!ReadFile(hPipe,chRequest+totalRead,4096-totalRead,&cbBytesRead,NULL) || cbBytesRead == 0) break;
				totalRead += cbBytesRead;
				char *szStrPtr = StrStrI(chRequest,"/END_PACKET");
				if(szStrPtr)
				{
					szStrPtr[0] = 0;
					break;
				}
			}

			if(totalRead)
			{
				chRequest[totalRead] = 0;
				
				if(StrCmpI(chRequest,"GetText") == 0)
				{
					writeLogFile("GetText");
					
					EnterCriticalSection(&grabDataSection);
					
					if(compiletostringline)
					{

						int count = wordsInfoArr.Count();
						for(int j=1;j<=count;j++)
						{
							words_info *wi = (words_info *)wordsInfoArr.Get(j);
							
							int count2 = wi->words->Count();
							for(int i=1;i <= count2;i++)
							{
								word_info *w = (word_info *)wi->words->Get(i);
								WriteFile(hPipe,w->word,lstrlen(w->word),&bw,0);
							}
							
							WriteFile(hPipe,"\r\n",2,&bw,0);
						}
					}
					else
					{



					int dataSize = lstrlen(szTextData);
					
					WriteFile(hPipe,szTextData,dataSize,&bw,0);


					}			
					LeaveCriticalSection(&grabDataSection);
					
					FlushFileBuffers(hPipe); 
					writeLogFile(szTextData);
				}
				else if(StrCmpI(chRequest,"DropGrabText") == 0)
				{
					writeLogFile("DropGrabText");
					EnterCriticalSection(&grabDataSection);

					if(compiletostringline)
					{
						int count = wordsInfoArr.Count();

						for(;count;count--)
						{
							words_info *wi = (words_info *)wordsInfoArr.Get(count);
							int count2 = wi->words->Count();

							for(;count2;count2--)
							{
								word_info *w = (word_info *)wi->words->Get(count2);
								MyFree(w->word);

							}
							
							wi->words->Empty();

							delete wi->words;

							MyFree(wi);

							
						}
						wordsInfoArr.Empty();


					}
					else
					{

					MyFree(szTextData);
					szTextData = 0;
					}

					wordsArr.Empty();

					LeaveCriticalSection(&grabDataSection);
					
				}
				else if(StrStrI(chRequest,"getStringXY"))
				{
					OutputDebugString("HOOK_DLL: getStringXY cmd");
					OutputDebugString(chRequest);
					char *szStrData = chRequest+11;
					szStrData[0] = 0;
					szStrData++;
					UINT StrHash = CalcHash(szStrData);
					DWORD bw;
					BOOL isContinue = true;

					int count = wordsInfoArr.Count();
					for(int j=1;j<=count && isContinue;j++)
					{
						words_info *wi = (words_info *)wordsInfoArr.Get(j);
						
						int count2 = wi->words->Count();
						for(int i=1;i <= count2;i++)
						{
							word_info *w = (word_info *)wi->words->Get(i);
							if(w->hash != StrHash) continue;
							char szTmpBuf[50];
							wsprintf(szTmpBuf,"%ux%u",w->x,w->y);
							WriteFile(hPipe,szTmpBuf,lstrlen(szTmpBuf),&bw,0);
							isContinue = false;
							break;
						}
							
					}


				}
			}

			
			DisconnectNamedPipe(hPipe);

		}
		
		CloseHandle(hPipe);

	}

	GlobalFree(chRequest);

	return 0;
}



void HideModule(HMODULE hModule)
{
    ULONG_PTR DllHandle = (ULONG_PTR)hModule;
    PPEB_LDR_DATA pebLdrData;
    PLDR_MODULE mod;

    __asm {
        mov eax, fs:[0x30]        //get PEB ADDR
        add eax, 0x0C        
        mov eax, [eax]            //get LoaderData ADDR
        mov pebLdrData, eax
    }

    for (
        mod = (PLDR_MODULE)pebLdrData->InLoadOrderModuleList.Flink;
        mod->BaseAddress != 0;
        mod = (PLDR_MODULE)mod->InLoadOrderModuleList.Flink
    ) {
        if ((HMODULE)mod->BaseAddress == hModule) {
            CUT_LIST(mod->InLoadOrderModuleList);
            CUT_LIST(mod->InInitializationOrderModuleList);
            CUT_LIST(mod->InMemoryOrderModuleList);
        
            ZeroMemory(mod, sizeof(LDR_MODULE));
            return;
        }
    }
}

