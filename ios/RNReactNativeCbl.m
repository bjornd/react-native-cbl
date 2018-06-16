#import "RNReactNativeCbl.h"
#import <Couchbaselite/CouchbaseLite.h>
#import <AssetsLibrary/AssetsLibrary.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import "RCTUIManager.h"
#import "RCTImageView.h"

@implementation RNReacNativeCbl

RCT_EXPORT_MODULE(RNReactNativeCbl)

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"liveQueryChange", @"liveDocumentChange"];
}

RCT_EXPORT_METHOD(openDb:(nonnull NSString*)name
                  installPrebuildDb:(BOOL)installPrebuildDb
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!_db) {
        /*if (installPrebuildDb) {
            CBLDatabase* db = [manager existingDatabaseNamed:name error:nil];
            if (db == nil) {
                NSString* dbPath = [[NSBundle mainBundle] pathForResource:name ofType:@"cblite2"];
                [manager replaceDatabaseNamed:name withDatabaseDir:dbPath error:nil];
            }
        }*/
        NSError *error;
        _db = [[CBLDatabase alloc] initWithName:name error:&error];
        if (!_db) {
            reject(@"open_database", @"Cannot open database", error);
        } else {
            resolve(@"ok");
        }
    } else {
        resolve(@"ok");
    }
}

RCT_EXPORT_METHOD(getDocument:(nonnull NSString*)docId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLDocument* doc = [_db documentWithID:docId];
    if (doc) {
        resolve( [self serializeDocument:doc] );
    } else {
        reject( @"document_not_found", @"Document not found", nil );
    }
}

RCT_EXPORT_METHOD(createDocument:(NSDictionary *)properties
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLMutableDocument* doc = [[CBLMutableDocument alloc] init];
    [doc setValuesForKeysWithDictionary:properties];
    NSError* error;
    if (![_db saveDocument:doc error:&error]) {
        reject(@"document_create", @"Can not create document", error);
    } else {
        resolve(doc.id);
    }
}

RCT_EXPORT_METHOD(updateDocument:(NSString*)docId
                  properties:(NSDictionary *)properties
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLMutableDocument* doc = [[_db documentWithID:docId] toMutable];
    if (!doc) {
       reject(@"document_update", @"Can not find document", nil);
       return;
    }
    [doc setValuesForKeysWithDictionary:properties];
    NSError* error;
    if (![_db saveDocument:doc error:&error]) {
        reject(@"document_update", @"Can not update document", error);
    } else {
        resolve(@"ok");
    }
}

RCT_EXPORT_METHOD(deleteDocument:(NSString*)docId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLDocument* doc = [_db documentWithID: docId];
    NSError* error;
    if (![_db deleteDocument:doc error:&error]) {
        reject(@"document_delete", @"Can not delete document", error);
    } else {
        resolve([NSNull null]);
    }
}

RCT_EXPORT_METHOD(createLiveDocument:(nonnull NSString*)docId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLDocument* doc = [_db documentWithID:docId];
    NSString *uuid = [[NSUUID UUID] UUIDString];
    __weak typeof(self) weakSelf = self;
    [_db addDocumentChangeListenerWithID:docId listener:^(CBLDocumentChange *change) {
        NSDictionary *data = [weakSelf serializeDocument:[weakSelf.db documentWithID:docId]];
        [weakSelf sendEventWithName:@"liveDocumentChange" body:@{ @"data": data, @"uuid": uuid }];
    }];
    [_liveDocuments setValue:doc forKey:uuid];
    resolve(uuid);
    [self sendEventWithName:@"liveDocumentChange" body:@{ @"data": [self serializeDocument:doc], @"uuid": uuid }];
}

RCT_EXPORT_METHOD(destroyLiveDocument:(nonnull NSString*)uuid
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [_liveDocuments removeObjectForKey:uuid];
    resolve(@"ok");
}

RCT_EXPORT_METHOD(query:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    NSError *error;
    CBLQuery *query = [CBLQueryBuilder select:@[[CBLQuerySelectResult all]]
                                         from:[CBLQueryDataSource database:_db]];
    CBLQueryResultSet *result = [query execute:&error];
    NSArray *data = [self getQueryResults:result];
    resolve(data);
}

RCT_EXPORT_METHOD(createLiveQuery:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLQuery *query = [CBLQueryBuilder select:@[[CBLQuerySelectResult all], [CBLQuerySelectResult expression: CBLQueryMeta.id]]
                                         from:[CBLQueryDataSource database:_db]];
    NSString *uuid = [[NSUUID UUID] UUIDString];
    [query addChangeListener:^(CBLQueryChange *change) {
        NSArray *data = [self getQueryResults:[change results]];
        [self sendEventWithName:@"liveQueryChange" body:@{ @"data": data, @"uuid": uuid }];
    }];

    if (!_liveQueries) {
        _liveQueries = [[NSMutableDictionary alloc] init];
    }
    [_liveQueries setValue:query forKey:uuid];
    resolve(uuid);

    NSError *error;
    CBLQueryResultSet *result = [query execute:&error];
    [self sendEventWithName:@"liveQueryChange" body:@{ @"data": [self getQueryResults:result], @"uuid": uuid }];
}

RCT_EXPORT_METHOD(destroyLiveQuery:(nonnull NSString*)uuid
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [_liveQueries removeObjectForKey:uuid];
    resolve(@"ok");
}

- (NSArray *)getQueryResults:(CBLQueryResultSet *)resultSet
{
    NSArray *rows = [resultSet allResults];
    NSMutableArray *mappedRows = [NSMutableArray arrayWithCapacity:[rows count]];
    [rows enumerateObjectsUsingBlock:^(CBLQueryResult *row, NSUInteger idx, BOOL *stop) {
        NSMutableDictionary *props = [[NSMutableDictionary alloc] initWithDictionary:[row toDictionary]];
        NSDictionary *rowProps = [[row valueForKey:_db.name] toDictionary];
        [props removeObjectForKey:_db.name];
        [props setValuesForKeysWithDictionary:rowProps];
        [mappedRows addObject:props];
    }];
    return mappedRows;
}

- (NSDictionary *)serializeDocument:(CBLDocument *)document
{
    NSMutableDictionary *properties = [[NSMutableDictionary alloc] initWithDictionary:[document toDictionary]];
    for (NSString* key in properties.allKeys) {
        if ([properties[key] isKindOfClass:[CBLBlob class]]) {
            CBLBlob *blob = (CBLBlob *)properties[key];
            NSMutableDictionary *blobProps = [[NSMutableDictionary alloc] initWithDictionary:blob.properties];
            NSString *fileName = [[[blob.digest substringFromIndex:5] stringByReplacingOccurrencesOfString:@"/" withString:@"_"] stringByAppendingString:@".blob"];
            NSString *filePath = [[_db.path stringByAppendingString:@"Attachments/"] stringByAppendingString:fileName];
            [blobProps setValue:[[NSURL fileURLWithPath:filePath] absoluteString] forKey:@"url"];
            [properties setValue:blobProps forKey:key];
        }
    }
    [properties setValue:document.id forKey:@"id"];
    return properties;
}

RCT_EXPORT_METHOD(startReplication:(NSString*)remoteUrl
                  facebookToken:(NSString*)facebookToken
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    NSURL *url = [[NSURL alloc] initWithString:remoteUrl];
    CBLURLEndpoint *targetEndpoint = [[CBLURLEndpoint alloc] initWithURL:url];
    CBLReplicatorConfiguration *replConfig = [[CBLReplicatorConfiguration alloc] initWithDatabase:_db target:targetEndpoint];
    replConfig.replicatorType = kCBLReplicatorTypePushAndPull;
    //replConfig.authenticator = [[CBLBasicAuthenticator alloc] initWithUsername:@"john" password:@"pass"];
    CBLReplicator *replicator = [[CBLReplicator alloc] initWithConfig:replConfig];
    [replicator start];
    resolve(@"ok");
}

RCT_EXPORT_METHOD(addAttachment:(NSString *)assetUri
                  named:(NSString *)attachmentName
                  toDocumentWithId:(NSString *)documentId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    ALAssetsLibrary *assetLibrary = [[ALAssetsLibrary alloc] init];
    NSURL *uri = [NSURL URLWithString:assetUri];
    [assetLibrary assetForURL:uri resultBlock:^(ALAsset *asset) {
        ALAssetRepresentation *rep = [asset defaultRepresentation];
        Byte *buffer = (Byte*)malloc(rep.size);
        NSUInteger buffered = [rep getBytes:buffer fromOffset:0.0 length:rep.size error:nil];
        NSData *data = [NSData dataWithBytesNoCopy:buffer length:buffered freeWhenDone:YES];
        NSString* mimeType = (__bridge_transfer NSString*)UTTypeCopyPreferredTagWithClass((__bridge CFStringRef)[rep UTI], kUTTagClassMIMEType);
        CBLMutableDocument* doc = [[_db documentWithID:documentId] toMutable];
        CBLBlob *blob = [[CBLBlob alloc] initWithContentType:mimeType data:data];
        [doc setBlob:blob forKey:attachmentName];
        NSError* error;
        if ([_db saveDocument:doc error:&error]) {
            resolve(@"ok");
        } else {
            reject(@"add_attachment", @"Can not add attachment", error);
        }
    } failureBlock:^(NSError *error) {
        reject(@"add_attachment", @"Can not add attachment", error);
    }];
}

RCT_EXPORT_METHOD(removeAttachment:(NSString *)attachmentName
                  fromDocumentWithId:(NSString *)documentId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLMutableDocument* doc = [[_db documentWithID:documentId] toMutable];
    [doc removeValueForKey:attachmentName];
    NSError* error;
    if ([_db saveDocument:doc error:&error]) {
        resolve(@"ok");
    } else {
        reject(@"remove_attachment", @"Can not remove attachment", error);
    }
}

@end
