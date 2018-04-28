
#include "hook_dll.h"


BOOL xmlGetRootNode(IXMLDOMDocument* pXmlDoc,IXMLDOMElement **ele)
{
	if (pXmlDoc)
	{
		HRESULT hr = pXmlDoc->get_documentElement(ele);


		if(hr == S_OK)
			return TRUE;
	}
	return FALSE;
}


BOOL xmlGetPathedNodeList(IXMLDOMDocument* pXmlDoc,IXMLDOMNode *node, char* strPath, IXMLDOMNodeList **list)
{
	WCHAR* wnodeName = CharToWChar(strPath);
	
	HRESULT hr = node->selectNodes(wnodeName, list);
	GlobalFree(wnodeName);
	if (hr == S_OK)
		return TRUE;
	return FALSE;

}

BOOL xmlGetPathedChildNode(IXMLDOMNode *node, char* strPath,IXMLDOMNode **childNode)
{
	WCHAR* wnodeName = CharToWChar(strPath);
	HRESULT hr = node->selectSingleNode(wnodeName,childNode);
	GlobalFree(wnodeName);
	if(hr == S_OK)
		return TRUE;
	return FALSE;

}

BOOL xmlGetPathedChildNodeList(IXMLDOMNode *node, char* strPath,IXMLDOMNodeList **childNodes)
{
	WCHAR* wnodeName = CharToWChar(strPath);
	HRESULT hr = node->selectNodes(wnodeName,childNodes);
	GlobalFree(wnodeName);
	if(hr == S_OK)
		return TRUE;
	return FALSE;

}


BOOL xmlGetPathedNodeList2(IXMLDOMDocument* pXmlDoc,char* strPath, IXMLDOMNodeList **list)
{
	IXMLDOMElement *ele;
	if (!xmlGetRootNode (pXmlDoc,&ele))
		return FALSE;
	BOOL result = xmlGetPathedNodeList(pXmlDoc,(IXMLDOMNode *)ele,strPath,list);
	ele->Release();	
	return result;
}


BOOL xmlGetPathedNode(IXMLDOMDocument* pXmlDoc,char *strPath, IXMLDOMNode **node)
{
	IXMLDOMNodeList *list=NULL;
    HRESULT hr;
	
	if(xmlGetPathedNodeList2(pXmlDoc,strPath,&list))
	{
		long len =0;
		hr = list->get_length (&len);
		if ( FAILED(hr))
		{
			list->Release ();
			return FALSE;
		}
		if ( len >0 )
		{
			hr = list->get_item (0,node);
			if ( FAILED(hr))
			{
				if ( *node )
					(*node)->Release ();
				list->Release ();
				return FALSE;
			}
			list->Release ();
			return TRUE;
		}
	 }
	if ( list )
		list->Release ();

	return FALSE;
}



IXMLDOMDocument2* xmlInit()
{
	IXMLDOMDocument2	*pXmlDoc;
	CoInitialize(NULL); //CLSID_DOMDocumentCLSID_DOMDocument30
	HRESULT hr = CoCreateInstance(CLSID_DOMDocument2, NULL, CLSCTX_INPROC_SERVER,IID_IXMLDOMDocument2, (void**)&pXmlDoc);
	if (hr == S_OK)
	{
		BSTR tmp = CharToWChar("SelectionLanguage");
		VARIANT vt;
		BSTR bstrValue = CharToWChar("XPath");
		V_BSTR(&vt) = SysAllocString(bstrValue);
		V_VT(&vt) = VT_BSTR;
		pXmlDoc->setProperty(tmp,vt);
		return pXmlDoc;
	}
	return FALSE;
}


void xmlFree(IXMLDOMDocument* pXmlDoc)
{
	if(pXmlDoc)
		pXmlDoc->Release();
	
}


IXMLDOMNode* xmlGetNode(IXMLDOMDocument* pXmlDoc,char *szPath)
{
	IXMLDOMNode* DOMNodePtr;
	WCHAR* wnodeName = CharToWChar(szPath);
	HRESULT hr = pXmlDoc->selectSingleNode(wnodeName,&DOMNodePtr);
	GlobalFree(wnodeName);
	if(hr == S_OK)
			return DOMNodePtr;
	
	return FALSE;
}


BOOL xmlAppendChildNode(IXMLDOMDocument* pXmlDoc,char* szPath,IXMLDOMNode* childNode)
{
	IXMLDOMNode* DOMNodePtr = xmlGetNode(pXmlDoc,szPath);
	if(DOMNodePtr)
	{
		HRESULT hr = DOMNodePtr->appendChild(childNode,0);
		DOMNodePtr->Release();
		if(hr == S_OK)
			return TRUE;
	}

	else
	{
		int pathSize = lstrlen(szPath);
		char *pathBuf = (char*)GlobalAlloc(GPTR,pathSize+1);
		RtlMoveMemory(pathBuf,szPath,pathSize);
		BOOL status = xmlAddNodeRecursion(pXmlDoc,pathBuf,childNode);
		GlobalFree(pathBuf);
		return status;
	}
	
	return FALSE;
}


BOOL xmlSaveLoadFile(LPCTSTR fileName,IXMLDOMDocument* pXmlDoc,xmlFlags flag)
{
	HRESULT hr;
	WCHAR buf[MAX_PATH+MAX_PATH+2];
	VARIANT_BOOL success = VARIANT_TRUE;
	VARIANT vt;
	BSTR urlPtr;
	
	if(fileName == 0)
	{
		pXmlDoc->get_url(&urlPtr);
		V_BSTR(&vt) = SysAllocString(urlPtr);
	}
	else
	{
		MultiByteToWideChar(CP_ACP,0,fileName,-1,buf,MAX_PATH+MAX_PATH+2);
		V_BSTR(&vt) = SysAllocString(buf);
	}

	V_VT(&vt) = VT_BSTR;

	if(flag == xmlFlagLoad)
	{
		pXmlDoc->put_async(VARIANT_FALSE);
		hr = pXmlDoc->load(vt,&success);
	}
	else
	{
		hr = pXmlDoc->save(vt);
		
	}
	
	SysFreeString(vt.bstrVal);
	if (FAILED(hr) || success != VARIANT_TRUE)
		return FALSE;

	return TRUE;
}


BOOL xmlLoad(char* xmlData,IXMLDOMDocument* pXmlDoc)
{
	HRESULT			hr;
	VARIANT_BOOL	success=0;
	BOOL			result = FALSE;

	

	int xmlSize = lstrlen(xmlData)+2;
	WCHAR* wxmlData = (WCHAR*)GlobalAlloc(GPTR,xmlSize*2);
	MultiByteToWideChar(CP_ACP,0,xmlData,-1,wxmlData,xmlSize);
	hr = pXmlDoc->loadXML(wxmlData,&success);
	GlobalFree(wxmlData);
	if (!FAILED(hr) || success == VARIANT_TRUE)
	{
		result = TRUE;
	}
	
	

	return result;
}

BOOL xmlLoad(WCHAR* wxmlData,IXMLDOMDocument* pXmlDoc)
{
	HRESULT			hr;
	VARIANT_BOOL	success=0;

	hr = pXmlDoc->loadXML(wxmlData,&success);
	if (!FAILED(hr) || success == VARIANT_TRUE)
	{
		return TRUE;
	}
	
	return FALSE;
}



IXMLDOMNodeList* xmlFindFirst(char* nodeName,IXMLDOMDocument* pXmlDoc)
{
	IXMLDOMNodeList* NodeListPtr;

	WCHAR* wnodeName = CharToWChar(nodeName);
	pXmlDoc->getElementsByTagName(wnodeName,&NodeListPtr);
	GlobalFree(wnodeName);

	return NodeListPtr;
}


char *xmlGetProperty(char *nodeName,IXMLDOMDocument* pXmlDoc)
{
	IXMLDOMNode* node;
	WCHAR* wnodeName = CharToWChar(nodeName);
	HRESULT hr = pXmlDoc->selectSingleNode(wnodeName,&node);
	GlobalFree(wnodeName);
	if (hr == S_OK)
	{
		char *retData = xmlGetNodeValue(node);
		node->Release();
		return retData;
	}
	return FALSE;
}


IXMLDOMNode* xmlFindNext(IXMLDOMNodeList* NodeList)
{
	IXMLDOMNode* nextItem;
	
	NodeList->nextNode(&nextItem);
	return nextItem;
}


IXMLDOMNodeList* xmlFindFirstChild(IXMLDOMNode* DOMNodePtr)
{
	IXMLDOMNodeList* NodeListPtr;

	DOMNodePtr->get_childNodes(&NodeListPtr);
	return NodeListPtr;
}


IXMLDOMNode* xmlFindNextChild(IXMLDOMNodeList* DOMNodeListPtr)
{
	IXMLDOMNode* NodePtr;

	DOMNodeListPtr->nextNode(&NodePtr);
	return NodePtr;
}


char *xmlGetNodeName(IXMLDOMNode* DOMNodePtr)
{
	BSTR tmp;
	DOMNodePtr->get_baseName(&tmp);
	return WCharToChar(tmp);
}


char *xmlGetNodeValue(IXMLDOMNode* DOMNodePtr)
{
	BSTR tmp;
	if(!DOMNodePtr) return 0;
	
	DOMNodePtr->get_text(&tmp);

	return WCharToChar(tmp);
}


IXMLDOMNode* xmlCreateNode(IXMLDOMDocument* pXmlDoc, int type, char* nodeName)
{
	IXMLDOMNode *node;
	VARIANT		vtype;
	
	BSTR bstrName	= CharToWChar(nodeName);
	vtype.vt		= VT_I4;
    V_I4(&vtype)	= (int)type;

    HRESULT hr = pXmlDoc->createNode(vtype, bstrName, (BSTR)"", &node);
	GlobalFree(bstrName);
	if(hr == S_OK)
	{
		return node;
	}
	
	return FALSE;
}


BOOL xmlAddNode(IXMLDOMDocument* pXmlDoc,IXMLDOMNode* pNode)
{

	IXMLDOMElement *ele;
	if (!xmlGetRootNode(pXmlDoc,&ele))
		return FALSE;

	HRESULT hr = ele->appendChild(pNode,0);
	ele->Release();
	if(hr == S_OK)
		return TRUE;


	return FALSE;
}


BOOL xmlSetNodeAttribute(IXMLDOMNode* pNode,char* name,char* value)
{
	IXMLDOMElement *elem;
	VARIANT vt;
	BSTR bstrName = CharToWChar(name);
	BSTR bstrValue = CharToWChar(value);
	V_BSTR(&vt) = SysAllocString(bstrValue);
	V_VT(&vt) = VT_BSTR;
	
	pNode->QueryInterface(IID_IXMLDOMElement,(void**)&elem);
	elem->setAttribute(bstrName,vt);

	GlobalFree(bstrName);
	GlobalFree(bstrValue);
	SysFreeString(vt.bstrVal);
	return 1;
}


BOOL xmlSetIntNodeAttribute(IXMLDOMNode* pNode,char* name,int value)
{
	IXMLDOMElement *elem;
	VARIANT vt;
	char szValue[250];

	BSTR bstrName = CharToWChar(name);
	wsprintf(szValue,"%u",value);
	BSTR bstrValue = CharToWChar(szValue);
	V_BSTR(&vt) = SysAllocString(bstrValue);
	V_VT(&vt) = VT_BSTR;
	
	pNode->QueryInterface(IID_IXMLDOMElement,(void**)&elem);
	elem->setAttribute(bstrName,vt);

	GlobalFree(bstrName);
	GlobalFree(bstrValue);
	SysFreeString(vt.bstrVal);
	return 1;
}


char *xmlGetXml(IXMLDOMDocument* pXmlDoc)
{
	BSTR xmlString;
	HRESULT hr = pXmlDoc->get_xml(&xmlString);
	if(hr == S_OK)
	{
		return WCharToChar(xmlString);
	}
	return FALSE;
}


char *xmlGetNodeXml(IXMLDOMNode* Node)
{
	BSTR xmlString;
	HRESULT hr = Node->get_xml(&xmlString);
	if(hr == S_OK)
	{
		return WCharToChar(xmlString);
	}
	return FALSE;
}


char* xmlGetNodeAttribute(IXMLDOMNode* pNode,char* name)
{
	IXMLDOMElement *elem;
	VARIANT vt;
	char *value=0;
	BSTR bstrName = CharToWChar(name);
	
	HRESULT hr = pNode->QueryInterface(IID_IXMLDOMElement,(void**)&elem);
	if(hr == S_OK)
	{
		hr = elem->getAttribute(bstrName,&vt);
		elem->Release();
		if(hr == S_OK)
			value = WCharToChar(vt.bstrVal);
	}	
	GlobalFree(bstrName);
	
	return value;
}


BOOL xmlGetIntNodeAttribute(IXMLDOMNode* pNode,char* name,int *value)
{
	char *retValue = xmlGetNodeAttribute(pNode,name);
	if(retValue)
	{
		*value = StrToInt(retValue);
		GlobalFree(retValue);
		return TRUE;
	}
	return FALSE;
}


BOOL xmlReplaceNode(IXMLDOMNode* oldNode,IXMLDOMNode* newNode)
{
	IXMLDOMNode* parentNode;//
	HRESULT hr = oldNode->get_parentNode(&parentNode);
	if(hr == S_OK)
	{
		hr = parentNode->replaceChild(newNode,oldNode,0);
		parentNode->Release();
		if(hr == S_OK)
			return TRUE;
	}

	return FALSE;
}


BOOL xmlSetNodeValue(IXMLDOMNode* Node,char *value)
{
	BSTR bstrValue = CharToWChar(value);
	HRESULT hr = Node->put_text(bstrValue);
	GlobalFree(bstrValue);
	if(hr == S_OK)
		return TRUE;
	return FALSE;

}


BOOL xmlReplacePathedNode(IXMLDOMDocument* pXmlDoc,char *nodeName,char *value)
{
	IXMLDOMNode		*Node;
	BOOL			status = FALSE;

	Node = xmlGetNode(pXmlDoc,nodeName);
	if(Node)
	{
		status = xmlSetNodeValue(Node,value);
		Node->Release();
	}
	else
	{
		char *nameOff = xmlFindLastName(nodeName);
		DWORD pathSize = nameOff-nodeName-1;
		char *pathBuf = (char*)GlobalAlloc(GPTR,pathSize+1);
		
		RtlMoveMemory(pathBuf,nodeName,pathSize);
		status = xmlAddPathedNode(pXmlDoc,pathBuf,nameOff,value,0);
		GlobalFree(pathBuf);
	}
	
	return status;
}


char *xmlFindLastName(char *nodePath)
{
	int size = lstrlen(nodePath);
	for(;size;size--)
	{
		if(nodePath[size] == '/')
			return &nodePath[++size];
	}
	return FALSE;
}


BOOL xmlDeletePathedNode(IXMLDOMDocument* pXmlDoc,char* nodePath)
{
	IXMLDOMNode* pNode = xmlGetNode(pXmlDoc,nodePath);
	if(pNode)
	{
		BOOL status = xmlDeleteNode(pNode);
		pNode->Release();
		return status;
	}
	return FALSE;
}


BOOL xmlDeleteNode(IXMLDOMNode* node)
{
	IXMLDOMNode* parentNode;
	HRESULT hr = node->get_parentNode(&parentNode);
	if(hr == S_OK)
	{
		hr = parentNode->removeChild(node,0);
		parentNode->Release();
		if(hr == S_OK)
			return TRUE;
	}
	return FALSE;
}


BOOL xmlAddPathedNode(IXMLDOMDocument* pXmlDoc,char* nodePath,char *szItemName,char *szData,IXMLDOMNode** outChildNode)
{
	BOOL status = FALSE;
	IXMLDOMNode* childNode;
	
	int pathSize = lstrlen(nodePath);
	char *pathBuf = (char*)GlobalAlloc(GPTR,pathSize+1);
	RtlMoveMemory(pathBuf,nodePath,pathSize);

	childNode = xmlCreateNode(pXmlDoc, NODE_ELEMENT,(szItemName)?szItemName:"item");
	if(childNode)
	{
		if(xmlSetNodeValue(childNode,szData))
			status = xmlAddNodeRecursion(pXmlDoc,pathBuf,childNode);
		if(outChildNode) *outChildNode = childNode;
		else childNode->Release();
	}
	
	GlobalFree(pathBuf);
	return status;
}


BOOL xmlAddNodeRecursion(IXMLDOMDocument* pXmlDoc,char *szPath,IXMLDOMNode* childNode)
{
	char			*parentItem;
	BOOL status		= FALSE;
	IXMLDOMNode		*parentNode;

	int pathSize = lstrlen(szPath);
	if(szPath[pathSize-1] == '/')
	{
		IXMLDOMElement *rootNode;
		if (!xmlGetRootNode (pXmlDoc,&rootNode))
		{	
			return FALSE;
		}
		
		HRESULT hr = ((IXMLDOMNode *)rootNode)->appendChild(childNode,0);
		rootNode->Release();
		if(hr == S_OK)
			return TRUE;
			
		return FALSE;	
	}
	else
	{
		parentNode = xmlGetNode(pXmlDoc,szPath);
		if(parentNode)
		{
			HRESULT hr = parentNode->appendChild(childNode,0);
			if(hr == S_OK)
				return TRUE;
		}
		else
		{
			parentItem = xmlFindLastName(szPath);
			if(parentItem)
			{
				char *szPtr = parentItem;
				szPtr--;
				*(char*)szPtr = 0;
				parentNode = xmlCreateNode(pXmlDoc, NODE_ELEMENT,parentItem);
				if(parentNode)
				{
					HRESULT hr = parentNode->appendChild(childNode,0);
					if(hr == S_OK)
						status = xmlAddNodeRecursion(pXmlDoc,szPath,parentNode);
					parentNode->Release();
				}
			}
		}
	}
	
	return status;
}


BOOL xmlGetIntNodeValue(IXMLDOMDocument* pXmlDoc,char *szPath,int *outValue)
{
	IXMLDOMNode		*pNode;

	pNode = xmlGetNode(pXmlDoc,szPath);
	if(pNode)
	{
		char *szValue = xmlGetNodeValue(pNode);
		if(szValue)
		{
			*outValue = StrToInt(szValue);
			GlobalFree(szValue);
			pNode->Release();
			return TRUE;
		}
	}

	return FALSE;
}


BOOL xmlWriteFile(char *filePath,IXMLDOMDocument2* pXmlDoc)
{
	DWORD bw;
	BOOL status = FALSE;
	char* xmlData = xmlGetXml(pXmlDoc);
	if(xmlData)
	{
		int size = lstrlen(xmlData);
		HANDLE hFile = CreateFile(filePath,GENERIC_READ|GENERIC_WRITE,FILE_SHARE_READ,0,OPEN_ALWAYS,FILE_ATTRIBUTE_SYSTEM,0);
		if(hFile != INVALID_HANDLE_VALUE)
		{
;
			WriteFile(hFile,xmlData,size,&bw,0);
			FlushFileBuffers(hFile);
			GlobalFree(xmlData);
			SetEndOfFile(hFile);
			CloseHandle(hFile);
			status = TRUE;
		}
	}

	return status;
}


IXMLDOMDocument2* xmlLoadFile(char *filePath)
{
	IXMLDOMDocument2* pRetXmlDoc = 0;

	HANDLE hFile = CreateFile(filePath,GENERIC_READ|GENERIC_WRITE,FILE_SHARE_READ,0,OPEN_EXISTING,FILE_ATTRIBUTE_SYSTEM,0);
	if(hFile != INVALID_HANDLE_VALUE)
	{
		int fSize = GetFileSize(hFile,0);
		char *rBuf;
		BOOL local = FALSE;

		if(fSize > 0)
		{
			rBuf = (char*)GlobalAlloc(GPTR,fSize+1);
			DWORD br;
			ReadFile(hFile,rBuf,fSize,&br,0);
		}
		else
		{
			local = TRUE;
			rBuf = "<?xml version='1.0'?><root/>";
			fSize = lstrlen(rBuf);
		}
		CloseHandle(hFile);
		
		pRetXmlDoc = xmlInit();
		if(pRetXmlDoc)
		{
			if(!xmlLoad(rBuf,pRetXmlDoc))
			{
				xmlFree(pRetXmlDoc);
				pRetXmlDoc = 0;
			}
		}

		if(!local)
			GlobalFree(rBuf);
	}

	return pRetXmlDoc;
}
