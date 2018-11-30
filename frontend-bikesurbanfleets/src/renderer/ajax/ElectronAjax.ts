import { Injectable } from '@angular/core';
import { Event } from 'electron';
import { HistoryEntitiesJson, HistoryTimeEntry } from '../../shared/history';
import { JsonValue } from '../../shared/util';
import { AjaxProtocol, BackendAjax, FormSchemaAjax, HistoryAjax, JsonLoaderAjax, SettingsAjax, CsvGeneratorAjax, MapDownloaderAjax } from './AjaxProtocol';
import { EntryPointDataType, MapDownloadArgs } from "../../shared/ConfigurationInterfaces";
import { CoreSimulatorArgs, UserGeneratorArgs } from "../../shared/BackendInterfaces";
import { JsonSchemaGroup, JsonInfo, JsonLayoutGroup } from '../../main/controllers/JsonLoaderController';
import { CsvArgs } from '../../main/controllers/CsvGeneratorController';

// https://github.com/electron/electron/issues/7300#issuecomment-274269710
const { ipcRenderer } = (window as any).require('electron');

function readIpc(channel: string, ...requestArgs: Array<any>): Promise<any> {
    ipcRenderer.send(channel, ...requestArgs);
    return new Promise((resolve, reject) => {
        ipcRenderer.on(channel, (event: Event, response: { status: number, data?: any }) => {
            ipcRenderer.removeAllListeners(channel);
            if (response.status === 200) {
                resolve(response.data);
            } else {
                reject(response.data);
            }
        });
    });
}

class ElectronHistory implements HistoryAjax {

    private static readonly IS_READY = new Error(`HistoryReady has already been initialized!`);
    private static readonly NOT_READY = new Error(`HistoryReader hasn't been initialized yet!`);

    private ready = false;

    async init(path: string): Promise<void> {
        if (this.ready) throw ElectronHistory.IS_READY;
        await readIpc('history-init', path);
        this.ready = true;
    }

    async close(): Promise<void> {
        if (!this.ready) throw ElectronHistory.NOT_READY;
        await readIpc('history-close');
        this.ready = false;
    }

    async getEntities(type: string): Promise<HistoryEntitiesJson> {
        if (!this.ready) throw ElectronHistory.NOT_READY;
        return await readIpc('history-entities', type);
    }

    async numberOFChangeFiles(): Promise<number> {
        if (!this.ready) throw ElectronHistory.NOT_READY;
        return await readIpc('history-nchanges');
    }

    async previousChangeFile(): Promise<Array<HistoryTimeEntry>> {
        if (!this.ready) throw ElectronHistory.NOT_READY;
        return await readIpc('history-previous');
    }

    async nextChangeFile(): Promise<Array<HistoryTimeEntry>> {
        if (!this.ready) throw ElectronHistory.NOT_READY;
        return await readIpc('history-next');
    }

    async getChangeFile(n: number): Promise<Array<HistoryTimeEntry>> {
        if (!this.ready) throw ElectronHistory.NOT_READY;
        return await readIpc('history-get', n);
    }

    async closeHistory(): Promise<void> {
        if(!this.ready) throw ElectronHistory.NOT_READY;
        await readIpc('history-close');
        this.ready = false;
    }

}

class ElectronSettings implements SettingsAjax {
    async get(property: string): Promise<any> {
        return await readIpc('settings-get', property);
    }

    async set(property: string, value: JsonValue): Promise<void> {
        await readIpc('settings-set', property, value);
    }
}

class ElectronFormSchema implements FormSchemaAjax {
    async init(): Promise<any> {
        return await readIpc('form-schema-init');
    }

    async getSchemaFormEntryPointAndUserTypes(): Promise<any> {
        return await readIpc('form-schema-entry-user-type');
    }

    async getSchemaByTypes(dataTypes: EntryPointDataType): Promise<any> {
        return await readIpc('form-schema-entry-point-by-type', dataTypes);
    }

    async getStationSchema(): Promise<any> {
        return await readIpc('form-schema-station');
    }

    async getGlobalSchema(): Promise<any> {
        return await readIpc('form-schema-global');
    }

}

class ElectronBackend implements BackendAjax {

    async init(): Promise<void> {
        return await readIpc('backend-call-init');
    }

    async generateUsers(args: UserGeneratorArgs): Promise<void> {
        return await readIpc('backend-call-generate-users', args);
    }

    async simulate(args: CoreSimulatorArgs): Promise<void> {
        return await readIpc('backend-call-core-simulation', args);
    }

    async cancelSimulation(): Promise<void> {
        return await readIpc('backend-call-cancel-simulation');
    }

    async closeBackend(): Promise<void> {
        return await readIpc('backend-call-close');
    }
}

class JsonLoader implements JsonLoaderAjax {

    async init(): Promise<void> {
        return await readIpc('json-loader-init');
    }

    async getAllSchemas(): Promise<JsonSchemaGroup> {
        return await readIpc('get-all-schemas');
    }

    async getAllLayouts(): Promise<JsonLayoutGroup> {
        return await readIpc('get-json-layout');
    }

    async loadJsonGlobal(path: string): Promise<any> {
        return await readIpc('load-json-global', path);
    }

    async loadJsonEntryPoints(path: string): Promise<any> {
        return await readIpc('load-json-entry-points', path);
    }

    async loadJsonStations(path: string): Promise<any> {
        return await readIpc('load-json-stations', path);
    }

    async writeJson(jsonInfo: JsonInfo): Promise<boolean> {
        return await readIpc('write-json', jsonInfo);
    }

    async close(): Promise<void> {
        return await readIpc('json-loader-close');
    }
}

class CsvGenerator implements CsvGeneratorAjax {

    async init(): Promise<void> {
        return await readIpc('csv-generator-init');
    }

    async writeCsv(args: CsvArgs): Promise<void> {
        return await readIpc('csv-generator-write', args);
    }

    async close(): Promise<void> {
        return await readIpc('csv-generator-close');
    }

}

class MapDownloader implements MapDownloaderAjax {
    
    async init(): Promise<void> {
        return await readIpc('map-download-init');
    }

    async download(args: MapDownloadArgs): Promise<void> {
        return await readIpc('map-download-get', args);
    }
    
    async cancel(): Promise<void> {
        return await readIpc('map-download-cancel');
    }

    async close(): Promise<void> {
        return await readIpc('map-download-close');
    }
    
}

@Injectable()
export class ElectronAjax implements AjaxProtocol {

    history: HistoryAjax;
    settings: SettingsAjax;
    formSchema: FormSchemaAjax;
    backend: ElectronBackend;
    jsonLoader: JsonLoaderAjax;
    csvGenerator: CsvGeneratorAjax;
    mapDownloader: MapDownloader;

    constructor() {
        this.history = new ElectronHistory();
        this.settings = new ElectronSettings();
        this.formSchema = new ElectronFormSchema();
        this.backend = new ElectronBackend();
        this.jsonLoader = new JsonLoader();
        this.csvGenerator = new CsvGenerator();
        this.mapDownloader = new MapDownloader();
    }
}
