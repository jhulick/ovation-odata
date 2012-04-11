package ovation.odata.model;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.odata4j.core.OEntityKey;
import org.odata4j.producer.QueryInfo;

import ovation.AnalysisRecord;
import ovation.DerivedResponse;
import ovation.Epoch;
import ovation.EpochGroup;
import ovation.Experiment;
import ovation.ExternalDevice;
import ovation.IAnnotatableEntityBase;
import ovation.IAnnotation;
import ovation.IEntityBase;
import ovation.IIOBase;
import ovation.IResponseDataBase;
import ovation.ITaggableEntityBase;
import ovation.ITimelineElement;
import ovation.KeywordTag;
import ovation.NumericData;
import ovation.NumericDataType;
import ovation.Project;
import ovation.Resource;
import ovation.Response;
import ovation.Source;
import ovation.Stimulus;
import ovation.URLResource;
import ovation.User;
import ovation.odata.model.dao.Property;
import ovation.odata.util.CollectionUtils;
import ovation.odata.util.DataContextCache;


public abstract class OvationModelBase<K,V extends IEntityBase> extends ExtendedPropertyModel<K,V> {
    public static final String GET_ALL_PQL = "true";	// apparently this PQL "query" returns all instances of a type - FIXME Ovation-specific

    protected OvationModelBase(Map<String,Class<?>> fieldTypes, Map<String,Class<?>> collectionTypes) {
    	super(fieldTypes, collectionTypes);
    }
    
	// rules of thumb - 
	// collections (association) can ONLY refer to other entity types 
	// properties (aggregation) can refer to primitive types and entity types
    
    /** register model handlers for all Ovation API model classes */
    public static void registerOvationModel() {
    	// top-level types
        ExtendedPropertyModel.addPropertyModel(new AnalysisRecordModel());
        ExtendedPropertyModel.addPropertyModel(new DerivedResponseModel());
        ExtendedPropertyModel.addPropertyModel(new EpochGroupModel());
        ExtendedPropertyModel.addPropertyModel(new EpochModel());
        ExtendedPropertyModel.addPropertyModel(new ExperimentModel());
        ExtendedPropertyModel.addPropertyModel(new ExternalDeviceModel());
        ExtendedPropertyModel.addPropertyModel(new KeywordTagModel());
        ExtendedPropertyModel.addPropertyModel(new ProjectModel());
        ExtendedPropertyModel.addPropertyModel(new ResourceModel());
        ExtendedPropertyModel.addPropertyModel(new ResponseModel());
        ExtendedPropertyModel.addPropertyModel(new SourceModel());
        ExtendedPropertyModel.addPropertyModel(new StimulusModel());
        ExtendedPropertyModel.addPropertyModel(new URLResourceModel());
        ExtendedPropertyModel.addPropertyModel(new UserModel());		
        
        // supporting types
        ExtendedPropertyModel.addPropertyModel(new StringModel());		// so we can return a collection of strings (they may have fixed this in odata4j 0.6)
        ExtendedPropertyModel.addPropertyModel(new DoubleModel());		// so we can return a collection of doubles (they may have fixed this in odata4j 0.6)
        ExtendedPropertyModel.addPropertyModel(new FloatModel());		// so we can return a collection of floats (they may have fixed this in odata4j 0.6)
        ExtendedPropertyModel.addPropertyModel(new IntegerModel());		// so we can return a collection of integers (they may have fixed this in odata4j 0.6)
        ExtendedPropertyModel.addPropertyModel(new LongModel());		// so we can return a collection of longs (they may have fixed this in odata4j 0.6)

        ExtendedPropertyModel.addPropertyModel(new Property.Model());	// so we can return string-string pairs
// FIXME        ExtendedPropertyModel.addPropertyModel(new NumericDataModel());
        ExtendedPropertyModel.addPropertyModel(new NumericDataTypeModel());	
        
        ExtendedPropertyModel.addPropertyModel(new ITaggableEntityBaseModel());	// dunno about this idea but we have a collection of base-types
        ExtendedPropertyModel.addPropertyModel(new IAnnotationModel());
    }
    
    // note, these two enums feel like they could be expanded to also have the getters but doing so would take a 
    // fair amount of time (tho might result in a very nice expendable design...)
    
    /** every property of every child-type - ensures consistent naming and also makes common util functions doable */
    protected enum PropertyName     {
        Owner(User.class), URI(String.class), UUID(String.class), IsIncomplete(Boolean.class),				// EntityBase
        Tag(String.class),	 																				// KeywordTag 
        																									// TaggableEntityBase 
        																									// AnnotatableEntityBase 
        EntryFunctionName(String.class), Name(String.class), Notes(String.class), Project(Project.class), 
        ScmRevision(String.class), ScmURL(String.class), SerializedLocation(String.class), 					// AnalysisRecord
        Experiment(Experiment.class), Manufacturer(String.class),  											// ExternalDevice + Name(String.class), SerializedLocation(String.class), 		
        Label(String.class), ParentSource(Source.class), ParentRoot(Source.class),  						// Source + SerializedLocation(String.class), 				
        Data(byte[].class), UTI(String.class), 																// Resource + Name(String.class), Notes(String.class),  			
        URL(String.class), 																					// URLResource
        ExternalDevice(ExternalDevice.class), Units(String.class), 											// IOBase
        Epoch(Epoch.class), PluginID(String.class),  														// Stimulus + SerializedLocation(String.class), 			
        NumericData(NumericData.class), NumericDataType(NumericDataType.class), 							// ResponseDataBase 	
        																									// Response + Epoch(ovation.Epoch.class), SerializedLocation(String.class), UTI(String.class), 										 			
        Description(String.class), 																			// DerivedResponse + Epoch(Epoch.class), Name(String.class), SerializedLocation(String.class), 			 		
        EndTime(LocalDateTime.class), StartTime(LocalDateTime.class), 										// TimelineElement 		
        EpochCount(Integer.class), ParentEpochGroup(EpochGroup.class), Source(Source.class),				// EpochGroup + Experiment(ovation.Experiment.class), Label(String.class), SerializedLocation(String.class),   			
        Duration(Double.class), EpochGroup(EpochGroup.class), ExcludeFromAnalysis(Boolean.class), 
        NextEpoch(Epoch.class), PreviousEpoch(Epoch.class), ProtocolID(String.class),						// Epoch + SerializedLocation(String.class)
        Purpose(String.class),																				// PurposeAndNotesEntity + Notes(String.class),
        																									// Experiment + SerializedLocation(String.class)
         																									// Project + Name(String.class), SerializedLocation(String.class)
        Username(String.class),																				// User
        Text(String.class),																					// IAnnotation
        
        
    	ByteOrder(String.class), Format(String.class), NumericByteOrder(String.class), SampleBytes(Short.class) // NumericDataType
        ;
        
    	final Class<?> _type;
    	PropertyName(Class<?> type) { _type = type; }
    	// ideas - base-class for both CollectionName and PropertyName enums, getter method factories (takes some work off model and adds consistency and centralizes code)
    };
    
    /** every collection (association) of every child-type - ensures consistent naming */
    protected enum CollectionName {
        MyProperties(Property.class), Owner(User.class), Properties(Property.class), ResourceNames(String.class), Resources(Resource.class),		// EntityBase
        Tagged(ITaggableEntityBase.class),																											// KeywordTag 			
        KeywordTags(KeywordTag.class), MyKeywordTags(KeywordTag.class), MyTags(String.class), Tags(String.class),									// TaggableEntityBase 
        AnnotationGroupTags(String.class), Annotations(IAnnotation.class), MyAnnotationGroupTags(String.class), MyAnnotations(IAnnotation.class),	// AnnotatableEntityBase
        AnalysisParameters(Property.class), Epochs(Epoch.class), 																					// AnalysisRecord
        																																			// ExternalDevice
        AllEpochGroups(EpochGroup.class), AllExperiments(Experiment.class), ChildLeafSourceDescendants(Source.class), SourceChildren(Source.class), 
        EpochGroups(EpochGroup.class), Experiments(Experiment.class), 																				// Source
        																																			// Resource
        																																			// URLResource
        DeviceParameters(Property.class), DimensionLabels(String.class),																			// IOBase
        StimulusParameters(Property.class), 																										// Stimulus
        DoubleData(Double.class), FloatData(Float.class), FloatingPointData(Double.class), IntData(Integer.class), IntegerData(Integer.class), 
        MatlabShape(Long.class), Shape(Long.class),																									// ResponseDataBase
        SamplingRates(Double.class), SamplingUnits(String.class), 																					// Response
        DerivationParameters(Property.class), 																										// DerivedResponse
        																																			// TimelineElement
        ChildLeafGroupDescendants(EpochGroup.class), GroupChildren(EpochGroup.class), EpochsUnsorted(Epoch.class), 									// EpochGroup + Epochs(Epoch.class), 
        AnalysisRecords(AnalysisRecord.class), DerivedResponses(DerivedResponse.class), DerivedResponseNames(String.class), 
        MyDerivedResponseNames(String.class), MyDerivedResponses(DerivedResponse.class), ProtocolParameters(Property.class), 
        Responses(Response.class), ResponseNames(String.class), StimuliNames(String.class), Stimuli(Stimulus.class),								// Epoch
        																																			// PurposeAndNotesEntity
        ExternalDevices(ExternalDevice.class), Projects(Project.class), Sources(Source.class),	// Experiment + EpochGroups(EpochGroup.class), Epochs(Epoch.class), 
        AnalysisRecordNames(String.class), MyAnalysisRecords(AnalysisRecord.class), MyAnalysisRecordNames(String.class), 							// Project + AnalysisRecords(AnalysisRecord.class), Experiments(Experiment.class),
    	Annotated(IAnnotatableEntityBase.class),																									// IAnnotation
        ;
        
    	final Class<?> _type;
    	CollectionName(Class<?> type) { _type = type; }
    };
    
    /** @return entity that matches key or null if none */
    public V getEntityByKey(OEntityKey key) {
        return (V)DataContextCache.getThreadContext().objectWithUUID(key.toKeyStringWithoutParentheses());
    }
    
    

    
    /**
     * model-hierarchy (if this changes this code must be updated)
     *  AnalysisRecord											-> AnnotatableEntityBase -> TaggableEntityBase -> EntityBase -> ooObj -> ooAbstractObj
     *  DerivedResp -> ResponseDataBase      -> IOBase 			-> AnnotatableEntityBase -> ...
     *  EpochGroup 							 -> TimelineElement -> AnnotatableEntityBase -> ...
     *  Epoch 								 -> TimelineElement -> ...
     *  Experiment 	-> PurposeAndNotesEntity -> TimelineElement -> ...
     *  ExternalDevi								   			-> AnnotatableEntityBase -> ...
     *  KeywordTag 																					  		   -> EntityBase -> ...
     *  Project 	-> PurposeAndNotesEntity -> ...
     *  Resource 									   			-> AnnotatableEntityBase -> ...
     *  Response 	-> ResponseDataBase      -> ...
     *  Source 										   			-> AnnotatableEntityBase -> ...
     *  Stimulus 							 -> IOBase          -> ...
     *  URLResource	-> Resource              -> ...
     * 
     * * ooAbstractObj
     * ** ooObj
     * *** EntityBase (MyProperties:<String,Object>[], Owner:User, Properties:<String,Object[]>[], ResourceNames:String[], Resources:Resource[], URI:String, UUID:String, IsComplete:bool)
     * **** KeywordTag (Tag:String, Tagged:TaggableEntityBase[])
     * **** TaggableEntityBase (KeywordTags:KeywordTag[], MyKeywordTags:KeywordTag[], MyTags:String[], Tags:String[]) 
     * ***** AnnotatableEntityBase (AnnotationGroupTags:String[], Annotations:IAnnotation[], MyAnnotationGroupTags:String[], MyAnnotations:IAnnotation[]) 
     * ****** AnalysisRecord (AnalysisParameters:<String,Object>[], EntryFunctionName:String, Epochs:Epoch[], Name:String, Notes:String, Project:Project, ScmRevision:String, ScmURL:String, SerializedLocation:String)
     * ****** ExternalDevice (Experiment:Experiment, Manufacturer:String, Name:String, SerializedLocation:String)
     * ****** Source (AllEpochGroups:EpochGroup[], AllExperiments:Experiment[], ChildLeafDescendants:Source[], Children:Source[], EpochGroups:EpochGroup[], Experiments:Experiment[], Label:String, Parent:Source, ParentRoot:Source, SerializedLocation:String)
     * ****** Resource (Data:byte[], Name:String, Notes:String, Uti:String)
     * ******* URLResource (URL:String)
     * ****** IOBase (DeviceParameters:<String,Object>[], DimensionLabels:String[], ExternalDevice:ExternalDevice, Units:String)
     * ******* Stimulus (Epoch:Epoch, PluginID:String, SerializedLocation:String, StimulusParameters:<String,Object>[])
     * ******* ResponseDataBase (Data:NumericData, DataBytes:byte[], DoubleData:double[], FloatData:float[], FloatingPointData:double[], IntData:int[], IntegerData:int[], MatlabShape:long[], NumericDataType:NumericDataType, Shape:long[])
     * ******** Response (Epoch:Epoch, SamplingRates:double[], SamplingUnits:String[], SerializedLocation:String, UTI:String)
     * ******** DerivedResponse (DerivationParameters:<String,Object>[], Description:String, Epoch:Epoch, Name:String, SerializedLocation:String)
     * ****** TimelineElement (EndTime:DateTime, StartTime:DateTime)
     * ******* EpochGroup (ChildLeafDescendants:EpochGroup[], Children:EpochGroup[], EpochCount:int, Epochs:Epoch[], EpochsUnsorted:Epoch[], Experiment:Experiement, Label:String, Parent:EpochGroup, SerializedLocation:String, Source:Source)
     * ******* Epoch (AnalysisRecords:AnalysisRecord[], DerivedResponses:DerivedResponse[], DerivedResponseNames:String[], Duration:double, EpochGroup:EpochGroup, ExcludeFromAnalysis:bool, MyDerivedResponseNames:String[], MyDerivedResponses:DerivedResponse[], NextEpoch:Epoch, PreviousEpoch:Epoch, ProtocolID:String, ProtocolParameters:<String,Object>[], Responses:Response[], ResponseNames:String[], SerializedLocation:String, StimuliNames:String[], Stimuli:Stimulus[])
     * ******* PurposeAndNotesEntity[IOwnerNotes,IScientificPurpose] (Notes:String, Purpose:String)
     * ******** Experiment (EpochGroups:EpochGroup[], Epochs:Epoch[], ExternalDevices:ExternalDevice[], Projects:Project[], SerializedLocation:String, Sources:Source[])
     * ******** Project (AnalysisRecords:AnalysisRecord[], AnalysisRecordNames:String[], Experiments:Experiment[], MyAnalysisRecords:AnalysisRecord[], MyAnalysisRecordNames:String[], Name:String, SerializedLocation:String)
     */
    
    /**  
     * the add* utility methods are used during setup to configure the service metadata properly for each type; 
     * they add the properties and collections along with their types for each of the Ovation base-type classes.  
     * 
     * @param propertyTypeMap
     * @param collectionTypeMap
     */
    /** this method and addCollections leverages the type associated with the PropertyName (thus it's only stored in 1 place and is guaranteed consistent system-wide*/
    protected static void addProperties(Map<String,Class<?>> propertyTypeMap, PropertyName... props) {
    	for (PropertyName prop : props) {
    		propertyTypeMap.put(prop.name(), prop._type);
    	}
    }
    protected static void addCollections(Map<String,Class<?>> collectionTypeMap, CollectionName... cols) {
    	for (CollectionName col : cols) {
    		collectionTypeMap.put(col.name(), col._type);
    	}
    }
    
    /** these type-specific methods add the properties and collections for each type 
     * here and in the specific object-type models are the code that needs to change when the underlying
     * Ovation model changes
     */
    protected static void addEntityBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Owner, PropertyName.URI, PropertyName.UUID, PropertyName.IsIncomplete); 
        addCollections(collectionTypeMap, CollectionName.MyProperties, CollectionName.Properties, CollectionName.ResourceNames, CollectionName.Resources);    
        // no parent type within Ovation
    }    
    protected static void addKeywordTag(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Tag); 
        addCollections(collectionTypeMap, CollectionName.Tagged);   
        addEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addTaggableEntityBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
//    	addProperties (propertyTypeMap,   PropertyName.); 
        addCollections(collectionTypeMap, CollectionName.KeywordTags, CollectionName.MyKeywordTags, CollectionName.MyTags, CollectionName.Tags);   
        addEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addAnnotatableEntityBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
//    	addProperties (propertyTypeMap,   PropertyName.); 
        addCollections(collectionTypeMap, CollectionName.AnnotationGroupTags, CollectionName.Annotations, CollectionName.MyAnnotationGroupTags, CollectionName.MyAnnotations);   
        addTaggableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addAnalysisRecord(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.EntryFunctionName, PropertyName.Name, PropertyName.Notes, PropertyName.Project, PropertyName.ScmRevision, PropertyName.ScmURL, PropertyName.SerializedLocation); 
        addCollections(collectionTypeMap, CollectionName.AnalysisParameters, CollectionName.Epochs);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addExternalDevice(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Experiment, PropertyName.Manufacturer, PropertyName.Name, PropertyName.SerializedLocation); 
//        addCollections(collectionTypeMap, CollectionName.);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addSource(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Label, PropertyName.ParentSource, PropertyName.ParentRoot, PropertyName.SerializedLocation); 
        addCollections(collectionTypeMap, CollectionName.AllEpochGroups, CollectionName.AllExperiments, CollectionName.ChildLeafSourceDescendants, CollectionName.SourceChildren, CollectionName.EpochGroups, CollectionName.Experiments);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addResource(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Data, PropertyName.Name, PropertyName.Notes, PropertyName.UTI); 
//        addCollections(collectionTypeMap, CollectionName.);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addURLResource(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.URL); 
//        addCollections(collectionTypeMap, CollectionName.);
        addResource(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addIOBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.ExternalDevice, PropertyName.Units); 
        addCollections(collectionTypeMap, CollectionName.DeviceParameters, CollectionName.DimensionLabels);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addStimulus(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Epoch, PropertyName.PluginID, PropertyName.SerializedLocation); 
        addCollections(collectionTypeMap, CollectionName.StimulusParameters);
        addIOBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addResponseDataBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.NumericData, PropertyName.Data, PropertyName.NumericDataType); 
        addCollections(collectionTypeMap, CollectionName.DoubleData, CollectionName.FloatData, CollectionName.FloatingPointData, CollectionName.IntData, CollectionName.IntegerData, CollectionName.MatlabShape, CollectionName.Shape);
        addIOBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addResponse(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Epoch, PropertyName.SerializedLocation, PropertyName.UTI); 
        addCollections(collectionTypeMap, CollectionName.SamplingRates, CollectionName.SamplingUnits);
        addResponseDataBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addDerivedResponse(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Description, PropertyName.Epoch, PropertyName.Name, PropertyName.SerializedLocation); 
        addCollections(collectionTypeMap, CollectionName.DerivationParameters);
        addResponseDataBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addTimelineElement(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.EndTime, PropertyName.StartTime); 
//        addCollections(collectionTypeMap, CollectionName.);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addEpochGroup(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.EpochCount, PropertyName.Experiment, PropertyName.Label, PropertyName.ParentEpochGroup, PropertyName.SerializedLocation, PropertyName.Source); 
        addCollections(collectionTypeMap, CollectionName.ChildLeafGroupDescendants, CollectionName.GroupChildren, CollectionName.Epochs, CollectionName.EpochsUnsorted);   
        addTimelineElement(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addEpoch(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Duration, PropertyName.EpochGroup, PropertyName.ExcludeFromAnalysis, PropertyName.NextEpoch, PropertyName.PreviousEpoch, PropertyName.ProtocolID, PropertyName.SerializedLocation); 
        addCollections(collectionTypeMap, CollectionName.AnalysisRecords, CollectionName.DerivedResponses, CollectionName.DerivedResponseNames, CollectionName.ProtocolParameters, CollectionName.Responses, CollectionName.ResponseNames, CollectionName.StimuliNames, CollectionName.Stimuli);   
        addTimelineElement(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addPurposeAndNotesEntity(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Notes, PropertyName.Purpose); 
//        addCollections(collectionTypeMap, CollectionName.);   
        addTimelineElement(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addExperiment(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.SerializedLocation); 
        addCollections(collectionTypeMap, CollectionName.EpochGroups, CollectionName.Epochs, CollectionName.ExternalDevices, CollectionName.Projects, CollectionName.Sources);   
        addPurposeAndNotesEntity(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addProject(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Name, PropertyName.SerializedLocation); 
        addCollections(collectionTypeMap, CollectionName.AnalysisRecords, CollectionName.AnalysisRecordNames, CollectionName.Experiments, CollectionName.MyAnalysisRecords, CollectionName.MyAnalysisRecordNames);   
        addPurposeAndNotesEntity(propertyTypeMap, collectionTypeMap);
    }    
    // User extends TaggableEntityBase
    protected static void addUser(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Username);
//      addCollections(collectionTypeMap, CollectionName.);   
    	
        addTaggableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    // IAnnotation extends ITaggableEntityBase
    protected static void addIAnnotation(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Text);
    	addCollections(collectionTypeMap, CollectionName.Annotated);
        addTaggableEntityBase(propertyTypeMap, collectionTypeMap);
    } 
    /* ResponseDataBase repeats much of this
    protected static void addNumericData(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.ByteOrder, PropertyName.DataBuffer, PropertyName.Data, PropertyName.DoubleData);

    	NumericData foo;
    	foo.get
    
    	getDataBuffer()
    	getDataBytes()	// byte[]
    	getDataFormat()
    	getDoubleData()
    	getFloatData()
    	getFloatingPointData()
    	getIntData()
    	getIntegerData()
    	getNumericByteOrder()
    	getSampleBytes()
    	getShape()
    	getShortIntData()
    	getUnsignedIntData()
    	addProperties (propertyTypeMap,   PropertyName.Text);
    	addCollections(collectionTypeMap, CollectionName.);

    }    
    */
    

    
    /**
     * the get* utility methods are used at request-processing time to reduce duplicate code 
     * across the various models (since they share so much in common in their base types).
     * not all base types are exposed as public classes, tho, so some of this code is still
     * in the child models even tho it's common to many of them.
     * 
     * @param obj
     * @param col
     * @return
     */
    // IEntityBase extends IooObj
    protected static Object getProperty(IEntityBase obj, PropertyName prop) {
    	switch (prop) {
    		case Owner:			return obj.getOwner();
    		case URI:			return obj.getURIString();	// always return String version - URI version just confuses odata4j
    		case UUID:			return obj.getUuid();
    		case IsIncomplete:	return obj.isIncomplete();
//    		case SerializedName:return obj.getSerializedName();
    		default: 			_log.error("Unknown property '" + prop + "' for type '" + obj + "'"); return null;	// nowhere to go from here
    	}
    }
    protected static Iterable<?> getCollection(IEntityBase obj, CollectionName col) {
    	switch (col) {
			case MyProperties:	return Property.makeIterable(obj.getMyProperties());
			case Properties:	return Property.makeIterable(obj.getProperties());
			case ResourceNames:	return CollectionUtils.makeIterable(obj.getResourceNames());
			case Resources:		return obj.getResourcesIterable();  
			default: 			_log.error("Unknown collection '" + col + "' for type '" + obj + "'"); return CollectionUtils.makeEmptyIterable();	// nowhere to go from here
		}
    }
    
    // KeywordTag extends EntityBase
    protected static Object getProperty(KeywordTag obj, PropertyName prop) {
    	switch (prop) {
    		case Tag: 	return obj.getTag();
    		default: 	return getProperty((IEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(KeywordTag obj, CollectionName col) {
    	switch (col) {
			case Tagged: 	return CollectionUtils.makeIterable(obj.getTagged());
			default: 		return getCollection((IEntityBase)obj, col);
		}
    }

    // TaggableEntityBase extends EntityBase implements ITaggableEntityBase
    protected static Object getProperty(ITaggableEntityBase obj, PropertyName prop) {
    	switch (prop) {
    		default: return getProperty((IEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(ITaggableEntityBase obj, CollectionName col) {
    	switch (col) {
			case KeywordTags: 	return CollectionUtils.makeIterable(obj.getKeywordTags());
			case MyKeywordTags: return CollectionUtils.makeIterable(obj.getMyKeywordTags());
			case MyTags: 		return CollectionUtils.makeIterable(obj.getMyTags());
			case Tags: 			return CollectionUtils.makeIterable(obj.getTags());
			default: 			return getCollection((IEntityBase)obj, col);
		}
    }

    // AnnotatableEntityBase extends TaggableEntityBase implements IAnnotatableEntityBase
    protected static Object getProperty(IAnnotatableEntityBase obj, PropertyName prop) {
    	switch (prop) {
    		default: return getProperty((ITaggableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(IAnnotatableEntityBase obj, CollectionName col) {
    	switch (col) {
			case AnnotationGroupTags: 	return CollectionUtils.makeIterable(obj.getAnnotationGroupTags());
			case Annotations:			return obj.getAnnotationsIterable();
			case MyAnnotationGroupTags: return CollectionUtils.makeIterable(obj.getMyAnnotationGroupTags());
			case MyAnnotations:			return obj.getMyAnnotationsIterable();
			default: 					return getCollection((ITaggableEntityBase)obj, col);
		}
    }
    
    // AnalysisRecord extends AnnotatableEntityBase
    protected static Object getProperty(AnalysisRecord obj, PropertyName prop) {
    	switch (prop) {
    		case EntryFunctionName:	return obj.getEntryFunctionName();
    		case Name:				return obj.getName();
    		case Notes:				return obj.getNotes();
    		case Project:			return obj.getProject();
    		case ScmRevision:		return obj.getScmRevision();
    		case ScmURL:			return convertURLToString(obj.getScmURL());
    		case SerializedLocation:return obj.getSerializedLocation();
    		default: 				return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(AnalysisRecord obj, CollectionName col) {
    	switch (col) {
			case AnalysisParameters:return Property.makeIterable(obj.getAnalysisParameters());
			case Epochs:			return obj.getEpochsIterable();
			default: 				return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }

    // ExternalDevice extends AnnotatableEntityBase
    protected static Object getProperty(ExternalDevice obj, PropertyName prop) {
    	switch (prop) {
    		case Experiment:		return obj.getExperiment();
    		case Manufacturer:		return obj.getManufacturer();
    		case Name:				return obj.getName();
    		case SerializedLocation:return obj.getSerializedLocation();
    		default: 				return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(ExternalDevice obj, CollectionName col) {
    	switch (col) {
			default: return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }

    // Source extends AnnotatableEntityBase
    protected static Object getProperty(Source obj, PropertyName prop) {
    	switch (prop) {
    		case Label:				return obj.getLabel();
    		case ParentSource: 		return obj.getParent();
    		case ParentRoot: 		return obj.getParentRoot();
    		case SerializedLocation:return obj.getSerializedLocation();
    		default: 				return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Source obj, CollectionName col) {
    	switch (col) {
			case AllEpochGroups: 			return CollectionUtils.makeIterable(obj.getAllEpochGroups());
			case AllExperiments: 			return CollectionUtils.makeIterable(obj.getAllExperiments());
			case ChildLeafSourceDescendants:return CollectionUtils.makeIterable(obj.getChildLeafDescendants());
			case SourceChildren: 			return CollectionUtils.makeIterable(obj.getChildren());
			case EpochGroups: 				return CollectionUtils.makeIterable(obj.getEpochGroups());
			case Experiments: 				return CollectionUtils.makeIterable(obj.getExperiments());
			default: 						return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }
    
    // Resource extends AnnotatableEntityBase
    protected static Object getProperty(Resource obj, PropertyName prop) {
    	switch (prop) {
    		case Data:	return obj.getData();
    		case Name:	return obj.getName();
    		case Notes:	return obj.getNotes();
    		case UTI:	return obj.getUti();
    		default: 	return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Resource obj, CollectionName col) {
    	switch (col) {
			default: return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }
    
    // URLResource extends Resource
    protected static Object getProperty(URLResource obj, PropertyName prop) {
    	switch (prop) {
    		case URL: return obj.getURLString();
    		default: return getProperty((Resource)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(URLResource obj, CollectionName col) {
    	switch (col) {
			default: return getCollection((Resource)obj, col);
		}
    }
    
    // IOBase extends AnnotatableEntityBase implements IIOBase
    protected static Object getProperty(IIOBase obj, PropertyName prop) {
    	switch (prop) {
    		case ExternalDevice:return obj.getExternalDevice();
    		case Units:			return obj.getUnits();
    		default: 			return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(IIOBase obj, CollectionName col) {
    	switch (col) {
			case DeviceParameters: 	return CollectionUtils.makeIterable(obj.getDeviceParameters());
			case DimensionLabels:	return CollectionUtils.makeEmptyIterable(); // obj.getDimensionLables();	// FIXME
			default: return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }
    
    // Stimulus extends IOBase 
    protected static Object getProperty(Stimulus obj, PropertyName prop) {
    	switch (prop) {
    		case Epoch:				return obj.getEpoch();
    		case PluginID:			return obj.getPluginID();
    		case SerializedLocation:return obj.getSerializedLocation();
    		default: 				return getProperty((IIOBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Stimulus obj, CollectionName col) {
    	switch (col) {
			case StimulusParameters: 	return Property.makeIterable(obj.getStimulusParameters());
			default: 					return getCollection((IIOBase)obj, col);
		}
    }
    
    // ResponseDataBase extends IOBase implements IResponseDataBase (FIXME but IResponseDataBase has no methods so it's pretty useless as an interface here) 
    protected static Object getProperty(IResponseDataBase obj, PropertyName prop) {
    	// since IResponseDataBase has no methods we need to cast back down to implementation type to get a handle to the methods :(
    	Response 		res = obj instanceof Response ? (Response)obj : null;
    	DerivedResponse dRes = obj instanceof DerivedResponse ? (DerivedResponse)obj : null;
    	if (res == null && dRes == null) {
    		_log.error("IResponseDataBase that isn't Response or DerivedResponse - can't use - " + obj);
    		return getProperty((IIOBase)obj, prop);
    	}
    	
    	try {
	    	switch (prop) {
	    		case NumericData:		return res != null ? res.getData() 				: dRes.getData();
	    		case Data:				return res != null ? res.getDataBytes() 		: dRes.getDataBytes();
	    		case NumericDataType:	return res != null ? res.getNumericDataType() 	: dRes.getNumericDataType();
	    		default: 				return getProperty((IIOBase)obj, prop);
	    	}
		} catch (RuntimeException ndx) { 
			// was NumericDataException, but that's no longer public (??)
			// there are several reasons this exception is thrown; not all of them are due to type-mismatch (some are just thrown because there's no data at all)
			return null;
		}
    }
    
    protected static Iterable<?> getCollection(IResponseDataBase obj, CollectionName col) {
    	Response 		res = obj instanceof Response ? (Response)obj : null;
    	DerivedResponse dRes = obj instanceof DerivedResponse ? (DerivedResponse)obj : null;
    	if (res == null && dRes == null) {
    		_log.error("IResponseDataBase that isn't Response or DerivedResponse - can't use - " + obj);
    		return getCollection((IIOBase)obj, col);
    	}
    	
    	switch (col) {
			case DoubleData:		return CollectionUtils.makeIterable(res != null ? res.getDoubleData() 		: dRes.getDoubleData());
			case FloatData:			return CollectionUtils.makeIterable(res != null ? res.getFloatData() 		: dRes.getFloatData());
			case FloatingPointData:	return CollectionUtils.makeIterable(res != null ? res.getFloatingPointData(): dRes.getFloatingPointData());
			case IntData:			return CollectionUtils.makeIterable(res != null ? res.getIntData() 			: dRes.getIntData());
			case IntegerData:		return CollectionUtils.makeIterable(res != null ? res.getIntegerData() 		: dRes.getIntegerData());
			case MatlabShape:		return CollectionUtils.makeIterable(res != null ? res.getMatlabShape() 		: dRes.getMatlabShape());
			case Shape:				return CollectionUtils.makeIterable(res != null ? res.getShape() 			: dRes.getShape());
			default: 				return getCollection((IIOBase)obj, col);
		}
    }
    
    // Response extends ResponseDataBase
    protected static Object getProperty(Response obj, PropertyName prop) {
    	switch (prop) {
    		case Epoch:				return obj.getEpoch();
    		case SerializedLocation:return obj.getSerializedLocation();
    		case UTI:				return obj.getUTI();
    		default: 				return getProperty((IResponseDataBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Response obj, CollectionName col) {
    	switch (col) {
			case SamplingRates:	return CollectionUtils.makeIterable(obj.getSamplingRates());
			case SamplingUnits: return CollectionUtils.makeIterable(obj.getSamplingUnits());
			default: 			return getCollection((IResponseDataBase)obj, col);
		}
    }
    
    // DerivedResponse extends ResponseDataBase (extends IOBase implements IResponseDataBase)
    protected static Object getProperty(DerivedResponse obj, PropertyName prop) {
    	switch (prop) {
    		case Description: 			return obj.getDescription();
    		case Epoch:					return obj.getEpoch();
    		case Name:					return obj.getName();
    		case SerializedLocation:	return obj.getSerializedLocation();
    		default: 					return getProperty((IResponseDataBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(DerivedResponse obj, CollectionName col) {
    	switch (col) {
			case DerivationParameters : return Property.makeIterable(obj.getDerivationParameters());
			default: 					return getCollection((IResponseDataBase)obj, col);
		}
    }
    
    
    // ITimelineElement extends IooObj, IEntityBase, ITaggableEntityBase, IAnnotatableEntityBase 
    protected static Object getProperty(ITimelineElement obj, PropertyName prop) {
    	switch (prop) {
			case EndTime:	return convertDateTime(obj.getEndTime());
			case StartTime:	return convertDateTime(obj.getStartTime());
   			default: 		return getProperty((IAnnotatableEntityBase)obj, prop); 
    	}
    }
    protected static Iterable<?> getCollection(ITimelineElement obj, CollectionName col) {
    	// has no collections
    	return getCollection((IAnnotatableEntityBase)obj, col);
    }
    
    // EpochGroup extends TimelineElement
    protected static Object getProperty(EpochGroup obj, PropertyName prop) {
    	switch (prop) {
    		case EpochCount:		return obj.getEpochCount();
    		case Experiment:		return obj.getExperiment();
    		case Label:				return obj.getLabel();
    		case ParentEpochGroup:	return obj.getParent();
    		case SerializedLocation:return obj.getSerializedLocation();
    		case Source:			return obj.getSource();
    		default: 				return getProperty((ITimelineElement)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(EpochGroup obj, CollectionName col) {
    	switch (col) {
			case ChildLeafGroupDescendants:	return CollectionUtils.makeIterable(obj.getChildLeafDescendants());
			case GroupChildren:				return CollectionUtils.makeIterable(obj.getChildren());
			case Epochs:					return CollectionUtils.makeIterable(obj.getEpochs());
			case EpochsUnsorted:   			return CollectionUtils.makeIterable(obj.getEpochsUnsorted());
			default: 						return getCollection((ITimelineElement)obj, col);
		}
    }
    
    // Epoch extends TimelineElement
    protected static Object getProperty(Epoch obj, PropertyName prop) {
    	switch (prop) {
    		case Duration:				return obj.getDuration();
    		case EpochGroup:			return obj.getEpochGroup();
    		case ExcludeFromAnalysis:	return obj.getExcludeFromAnalysis();
    		case NextEpoch:				return obj.getNextEpoch();
    		case PreviousEpoch:			return obj.getPreviousEpoch();
    		case ProtocolID:			return obj.getProtocolID();
    		case SerializedLocation:	return obj.getSerializedLocation();
    		default: 					return getProperty((ITimelineElement)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Epoch obj, CollectionName col) {
    	switch (col) {
			case AnalysisRecords:		return CollectionUtils.makeIterable(obj.getAnalysisRecords());
			case DerivedResponses:		return obj.getDerivedResponseIterable();
			case DerivedResponseNames:	return CollectionUtils.makeIterable(obj.getDerivedResponseNames());
			case ProtocolParameters:	return Property.makeIterable(obj.getProtocolParameters());
			case Responses:				return obj.getResponseIterable();
			case ResponseNames:			return CollectionUtils.makeIterable(obj.getResponseNames());
			case StimuliNames:			return CollectionUtils.makeIterable(obj.getStimuliNames());
			case Stimuli:   			return obj.getStimulusIterable();
			default: 					return getCollection((ITimelineElement)obj, col);
		}
    }
    
    // Experiment extends PurposeAndNotesEntity (extends TimelineElement (extends AnnotatableEntityBase implements ITimelineElement) implements IOwnerNotes, IScientificPurpose)
    protected static Object getProperty(Experiment obj, PropertyName prop) {
    	switch (prop) {
    		case SerializedLocation:	return obj.getSerializedLocation();
	    	case Notes:					return obj.getNotes();		// 2 different interfaces so easier to just handle it here (for now)
	    	case Purpose: 				return obj.getPurpose();
	    	default: 					return getProperty((ITimelineElement)obj, prop); 
    	}
    }    
    protected static Iterable<?> getCollection(Experiment obj, CollectionName col) {
    	switch (col) {
    		case EpochGroups:		return CollectionUtils.makeIterable(obj.getEpochGroups());
    		case Epochs:			return obj.getEpochIterable();
    		case ExternalDevices:	return CollectionUtils.makeIterable(obj.getExternalDevices());
    		case Projects:			return CollectionUtils.makeIterable(obj.getProjects());
    		case Sources:   		return CollectionUtils.makeIterable(obj.getSources());
    		default:				return getCollection((ITimelineElement)obj, col); 
    	
    	}
    }    
    
    // Project extends PurposeAndNotesEntity (extends TimelineElement (extends AnnotatableEntityBase implements ITimelineElement) implements IOwnerNotes, IScientificPurpose)
    protected static Object getProperty(Project obj, PropertyName prop) {
    	switch (prop) {
	    	case Name:					return obj.getName();
	    	case SerializedLocation:	return obj.getSerializedLocation();
	    	case Notes:					return obj.getNotes();		// 2 different interfaces so easier to just handle it here (for now)
	    	case Purpose: 				return obj.getPurpose();
	    	default: 					return getProperty((ITimelineElement)obj, prop); 
    	}
    }    
    protected static Iterable<?> getCollection(Project obj, CollectionName col) {
    	switch (col) {
    		case AnalysisRecords: 		return obj.getAnalysisRecordIterable();
    		case AnalysisRecordNames:	return CollectionUtils.makeIterable(obj.getAnalysisRecordNames());
    		case Experiments:			return CollectionUtils.makeIterable(obj.getExperiments());
    		case MyAnalysisRecords:		return obj.getMyAnalysisRecordIterable();
    		case MyAnalysisRecordNames:	return CollectionUtils.makeIterable(obj.getMyAnalysisRecordNames());
	    	default: 					return getCollection((ITimelineElement)obj, col); 
    	}
    }
    
    // User extends TaggableEntityBase
    protected static Object getProperty(User obj, PropertyName prop) {
    	switch (prop) {
	    	case Username:	return obj.getUsername();
	    	default: 		return getProperty((ITaggableEntityBase)obj, prop); 
    	}
    }    
    protected static Iterable<?> getCollection(User obj, CollectionName col) {
    	switch (col) {
	    	default: 					return getCollection((ITaggableEntityBase)obj, col); 
    	}
    }

    // IAnnotation extends ITaggableEntityBase
    protected static Object getProperty(IAnnotation obj, PropertyName prop) {
    	switch (prop) {
	    	case Text:	return obj.getText();
	    	default: 	return getProperty((ITaggableEntityBase)obj, prop); 
    	}
    }    
    protected static Iterable<?> getCollection(IAnnotation obj, CollectionName col) {
    	switch (col) {
    		case Annotated:	return CollectionUtils.makeIterable(obj.getAnnotated()); 
	    	default:		return getCollection((ITaggableEntityBase)obj, col); 
    	}
    }
    
    /* cast the Value type to a type which subclasses IEntityBase
     * @throws ClassCastException if the value-type doesn't extended from Ovations IEntityBase
     */
//    protected Class<? extends IEntityBase> getOvationEntityType() { return (Class<? extends IEntityBase>) getEntityType(); }
    
//    @SuppressWarnings("unchecked")
    protected Iterable<V> executeQueryInfo() {
        return (Iterable<V>)executeQueryInfo(getEntityType(), getQueryInfo());
    }
    
	protected Iterable<V> executeQuery(String query) {
    	return (Iterable<V>)CollectionUtils.makeIterable(executeQuery(getEntityType(), query));
    }
    
    protected static Iterable<? extends IEntityBase> executeQueryInfo(Class<? extends IEntityBase> type, QueryInfo info) {    // Iterator<EntityBase> FIXME
        // http://win7-32:8080/ovodata/Ovodata.svc/Projects/?$format=json&pql=query%20goes%20here        
        // queryInfo:{{inlineCnt:null, top:null, skip:null, filter:null, orderBy:null, skipToken:null, customOptions:{pql=query goes here}, expand:[], select:[]}
        Map<String,String> customOptions = (info != null && info.customOptions != null) ? info.customOptions : null;
        if (customOptions != null) {
            String pqlQuery = customOptions.get("pql");
            String entityUrl = customOptions.get("url");
            if (pqlQuery != null) {
                Iterator<? extends IEntityBase> iter = executeQuery((Class<? extends IEntityBase>)type, pqlQuery);
                _log.info("query '" + pqlQuery + "' = " + iter);
                return CollectionUtils.makeIterable(iter);
            } else
            if (entityUrl != null) {
            	IEntityBase obj = getByURI(entityUrl);
                _log.info("url '" + entityUrl + "' = " + obj);
                if (obj == null) return CollectionUtils.makeEmptyIterable();
                return CollectionUtils.makeIterable(obj);
            } else {
                _log.info("both pql and url params are null");
            }
        } else {
            _log.info("customOptions is null - info:" + info);
        }
        return null;
    }
    
	public static <T extends IEntityBase> Iterator<T> executeQuery(Class<? extends IEntityBase> type, String pqlQuery) {
    	return DataContextCache.getThreadContext().query(type, pqlQuery);
    }

    public static IEntityBase getByURI(String uri) { 
    	return DataContextCache.getThreadContext().objectWithURI(uri);
    }

	public static LocalDateTime 	convertDateTime(DateTime dt) { return dt != null ? dt.toLocalDateTime() : null; }
	public static String 			convertURLToString(URL url) { return url != null ? url.toExternalForm() : null; }
}
