

enum xmlFlags
{
	xmlFlagSave,
	xmlFlagLoad
};

enum xmlAtType
{
	xmlSTRING,
	xmlINTEGER,
	xmlDOUBLE
};


BOOL xmlLoad(WCHAR* wxmlData,IXMLDOMDocument* pXmlDoc);
BOOL xmlGetRootNode(IXMLDOMDocument* pXmlDoc,IXMLDOMElement **ele);
BOOL xmlGetPathedNodeList(IXMLDOMDocument* pXmlDoc,IXMLDOMNode *node, char* strPath, IXMLDOMNodeList **list);
BOOL xmlGetPathedNodeList2(IXMLDOMDocument* pXmlDoc,char* strPath, IXMLDOMNodeList **list);
BOOL xmlGetPathedNode(IXMLDOMDocument* pXmlDoc,char *strPath, IXMLDOMNode **node);
char *xmlGetXml(IXMLDOMDocument* pXmlDoc);
BOOL xmlGetIntNodeAttribute(IXMLDOMNode* pNode,char* name,int *value);
BOOL xmlReplaceNode(IXMLDOMNode* oldNode,IXMLDOMNode* newNode);
IXMLDOMNode* xmlGetNode(IXMLDOMDocument* pXmlDoc,char *szPath);
BOOL xmlAppendChildNode(IXMLDOMDocument* pXmlDoc,char* szPath,IXMLDOMNode* chilNode);
BOOL xmlSetNodeValue(IXMLDOMNode* Node,char *value);
BOOL xmlReplacePathedNode(IXMLDOMDocument* pXmlDoc,char *nodeName,char *value);
BOOL xmlDeleteNode(IXMLDOMNode* node);
BOOL xmlDeletePathedNode(IXMLDOMDocument* pXmlDoc,char* nodePath);
BOOL xmlAddNodeRecursion(IXMLDOMDocument* pXmlDoc,char *szPath,IXMLDOMNode* childNode);
BOOL xmlAddPathedNode(IXMLDOMDocument* pXmlDoc,char* nodePath,char *szItemName,char *szData,IXMLDOMNode** outChildNode);
BOOL xmlGetIntNodeValue(IXMLDOMDocument* pXmlDoc,char *szPath,int *outValue);
char *xmlGetNodeXml(IXMLDOMNode* Node);
char *xmlFindLastName(char *nodePath);
BOOL xmlSetIntNodeAttribute(IXMLDOMNode* pNode,char* name,int value);


BOOL xmlSetNodeAttribute(IXMLDOMNode* pNode,char* name,char* value);
char* xmlGetNodeAttribute(IXMLDOMNode* pNode,char* name);
BOOL xmlAddNode(IXMLDOMDocument* pXmlDoc,IXMLDOMNode* node);
IXMLDOMNode* xmlCreateNode(IXMLDOMDocument* pXmlDoc, int type, char* nodeName);
char* WCharToChar(WCHAR* wString);
WCHAR* CharToWChar(char* szString);
IXMLDOMNodeList* xmlFindFirst(char* nodeName,IXMLDOMDocument* pXmlDoc);
IXMLDOMNode* xmlFindNext(IXMLDOMNodeList* NodeList);
IXMLDOMNodeList* xmlFindFirstChild(IXMLDOMNode* DOMNodePtr);
IXMLDOMNode* xmlFindNextChild(IXMLDOMNodeList* DOMNodeListPtr);
char *xmlGetNodeName(IXMLDOMNode* DOMNodePtr);
char *xmlGetNodeValue(IXMLDOMNode* DOMNodePtr);
char *xmlGetProperty(char *nodeName,IXMLDOMDocument* pXmlDoc);
BOOL xmlSaveLoadFile(LPCTSTR fileName,IXMLDOMDocument* pXmlDoc,xmlFlags flag);
IXMLDOMDocument2* xmlInit();
void xmlFree(IXMLDOMDocument* pXmlDoc);
BOOL xmlLoad(char* xmlData,IXMLDOMDocument* pXmlDoc);
BOOL xmlGetPathedChildNode(IXMLDOMNode *node, char* strPath,IXMLDOMNode **childNode);




