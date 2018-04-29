#import "RNReactNativeCbl.h"
#import <Couchbaselite/CouchbaseLite.h>
#import "CBLRegisterJSViewCompiler.h"
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
        CBLRegisterJSViewCompiler();
        CBLManager* manager = [CBLManager sharedInstance];
        if (!manager) {
            reject(@"no_manager", @"Cannot create Manager instance", nil);
            return;
        }
        if (installPrebuildDb) {
            CBLDatabase* db = [manager existingDatabaseNamed:name error:nil];
            if (db == nil) {
                NSString* dbPath = [[NSBundle mainBundle] pathForResource:name ofType:@"cblite2"];
                [manager replaceDatabaseNamed:name withDatabaseDir:dbPath error:nil];
            }
        }
        NSError *error;
        _db = [manager databaseNamed:name error: &error];
        if (!_db) {
            reject(@"open_database", @"Cannot open database", error);
        } else {
            [[NSNotificationCenter defaultCenter] addObserver: self
                                                     selector: @selector(databaseChanged:)
                                                         name: kCBLDatabaseChangeNotification
                                                       object: _db];
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
    CBLDocument* doc = [_db createDocument];
    NSError* error;
    if (![doc putProperties: properties error: &error]) {
        reject(@"document_create", @"Can not create document", error);
    } else {
        resolve(doc.documentID);
    }
}

RCT_EXPORT_METHOD(updateDocument:(NSString*)docId
                  properties:(NSDictionary *)properties
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLDocument* doc = [_db documentWithID:docId];
    //if (!doc) {
    //    reject(@"document_update", @"Can not find document", nil);
    //    return;
    //}
    NSError* error;
    if (![doc update: ^BOOL(CBLUnsavedRevision *newRev) {
        for (id key in properties) {
            newRev[key] = properties[key];
        }
        return YES;
    } error: &error]) {
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
    if (![doc deleteDocument: &error]) {
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
    [[NSNotificationCenter defaultCenter] addObserverForName: kCBLDocumentChangeNotification
                                                      object: doc
                                                       queue: nil
                                                  usingBlock: ^(NSNotification *n) {
                                                      [self sendEventWithName:@"liveDocumentChange" body:@{ @"data": [self serializeDocument:doc], @"uuid": uuid }];
                                                  }
     ];
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

RCT_EXPORT_METHOD(query:(nonnull NSString*)view
                  params:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLView *dbview = [_db viewNamed:view];
    [dbview mapBlock];
    CBLQuery* query = [dbview createQuery];
    [query setValuesForKeysWithDictionary:params];
    NSError *error;
    CBLQueryEnumerator *enumerator = [query run:&error];
    NSArray *data = [self getQueryResults:enumerator];
    resolve(data);
}

RCT_EXPORT_METHOD(createLiveQuery:(nonnull NSString*)view
                  params:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLView *dbview = [_db viewNamed:view];
    [dbview mapBlock];
    CBLQuery* query = [dbview createQuery];
    NSString *uuid = [[NSUUID UUID] UUIDString];
    CBLLiveQuery *liveQuery = [query asLiveQuery];
    [liveQuery setValuesForKeysWithDictionary:params];
    [liveQuery addObserver:self forKeyPath:@"rows" options:0 context:NULL];
    [liveQuery start];
    if (!_liveQueries) {
        _liveQueries = [[NSMutableDictionary alloc] init];
    }
    [_liveQueries setValue:liveQuery forKey:uuid];
    resolve(uuid);
}

RCT_EXPORT_METHOD(destroyLiveQuery:(nonnull NSString*)uuid
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    CBLLiveQuery *liveQuery = [_liveQueries objectForKey:uuid];
    [liveQuery stop];
    [liveQuery removeObserver:self forKeyPath:@"rows"];
    [_liveQueries removeObjectForKey:uuid];
    resolve(@"ok");
}

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary *)change
                       context:(void *)context
{
    for (id key in _liveQueries) {
        if ([_liveQueries objectForKey:key] == object) {
            NSArray *data = [self getQueryResults:((CBLLiveQuery *)object).rows];
            [self sendEventWithName:@"liveQueryChange" body:@{ @"data": data, @"uuid": key }];
        }
    }
}

- (void)databaseChanged:(NSNotification*)n {
    for (CBLDatabaseChange* change in n.userInfo[@"changes"]) {
        for (id key in _liveQueries) {
            NSArray *rows = [((CBLLiveQuery *)[_liveQueries objectForKey:key]).rows allObjects];
            NSUInteger index = [rows indexOfObjectPassingTest:^BOOL(CBLQueryRow *obj, NSUInteger idx, BOOL *stop) {
                if ([obj.documentID isEqualToString:change.documentID]) {
                    return YES;
                }
                return NO;
            }];
            if (index != NSNotFound) {
                NSArray *data = [self getQueryResults:((CBLLiveQuery *)[_liveQueries objectForKey:key]).rows];
                [self sendEventWithName:@"liveQueryChange" body:@{ @"data": data, @"uuid": key }];
            }
        }
    }
}

- (NSArray *)getQueryResults:(CBLQueryEnumerator *)queryEnumerator
{
    NSArray *rows = [queryEnumerator allObjects];
    NSMutableArray *mappedRows = [NSMutableArray arrayWithCapacity:[rows count]];
    [rows enumerateObjectsUsingBlock:^(CBLQueryRow *obj, NSUInteger idx, BOOL *stop) {
        if (obj.document) {
            if (!obj.document.isDeleted) {
                NSDictionary *serializedDoc = [self serializeDocument:obj.document];
                if (serializedDoc) {
                    [mappedRows addObject:serializedDoc];
                }
            }
        } else {
            [mappedRows addObject:@{ @"key": obj.key, @"value": obj.value == nil ? [NSNull null] : obj.value }];
        }
    }];
    return mappedRows;
}

- (NSDictionary *)serializeDocument:(CBLDocument *)document
{
    NSMutableDictionary *properties = [[NSMutableDictionary alloc] initWithDictionary:document.properties];
    NSDictionary *attachments = [properties objectForKey:@"_attachments"];
    NSMutableDictionary *mappedAttachments = [[NSMutableDictionary alloc] initWithCapacity:attachments.count];
    for(id key in attachments) {
        NSMutableDictionary *attData = [[NSMutableDictionary alloc] initWithDictionary:[attachments objectForKey:key]];
        NSString *attUrl = [document.currentRevision attachmentNamed:key].contentURL.absoluteString;
        [attData setObject:attUrl forKey:@"url"];
        [mappedAttachments setObject:attData forKey:key];
    }
    [properties setObject:mappedAttachments forKey:@"_attachments"];
    return properties;
}

RCT_EXPORT_METHOD(startReplication:(NSString*)remoteUrl
                  facebookToken:(NSString*)facebookToken
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    NSURL* url = [NSURL URLWithString:remoteUrl];
    CBLReplication *push = [_db createPushReplication: url];
    CBLReplication *pull = [_db createPullReplication: url];
    push.continuous = pull.continuous = YES;
    //id<CBLAuthenticator> auth;
    //auth = [CBLAuthenticator facebookAuthenticatorWithToken:facebookToken];
    //push.authenticator = pull.authenticator = auth;
    [push start];
    [pull start];
    _push = push;
    _pull = pull;
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
        CBLDocument* doc = [_db documentWithID:documentId];
        CBLUnsavedRevision* newRev = [doc.currentRevision createRevision];
        NSString *name = attachmentName;
        if (name == nil) {
            name = [[NSUUID UUID] UUIDString];
        }
        [newRev setAttachmentNamed:name
                   withContentType:mimeType
                           content:data];
        NSError* error;
        if ([newRev save: &error]) {
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
    CBLDocument* doc = [_db documentWithID:documentId];
    CBLUnsavedRevision* newRev = [doc.currentRevision createRevision];
    [newRev removeAttachmentNamed:attachmentName];
    NSError* error;
    if ([newRev save: &error]) {
        resolve(@"ok");
    } else {
        reject(@"remove_attachment", @"Can not remove attachment", error);
    }
}

@end
