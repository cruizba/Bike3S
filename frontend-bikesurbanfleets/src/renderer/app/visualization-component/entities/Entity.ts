import { Icon, LeafletEvent } from 'leaflet';
import { Diff, Flatten, If, Is, Safe, UnArray } from '../../../../shared/mappedtypes';
import { Geo } from '../../../../shared/util';
import { LeafletUtil } from '../util';

type EntityCallback<T extends Entity, R> = (entity: T) => R;
type LeafletEventCallback<T extends Entity, E extends LeafletEvent> = (entity: T, event: E) => void;
type ReferenceCallback<T extends Entity, R extends Entity> = (entity: T, reference: R) => void;

type AllowedEvents = Flatten<LeafletUtil.MarkerEvents, 'Map' | 'Mouse' | 'Popup' | 'Tooltip'>;

export interface HistoricConfiguration<T extends Entity = any> {
    jsonIdentifier: string;
    marker?: {
        at: EntityCallback<T, Geo.Point | null> | {
            route: EntityCallback<T, Geo.Route | null>,
            speed: EntityCallback<T, number>,
        },
        when?: EntityCallback<T, boolean>,
        icon?: EntityCallback<T, Icon<any>>,
        popup?: EntityCallback<T, string>,
        on?: {
            [P in keyof AllowedEvents]?: LeafletEventCallback<T, AllowedEvents[P]>
        },
    };
    on?: {
        init?: EntityCallback<T, void>,
        // @ts-ignore
        propertyUpdate?: Partial<Record<Diff<keyof T, keyof Entity>, EntityCallback<T, void>>>,
        update?: EntityCallback<T, void>,
        referenceUpdate?: References<T>,
    };
}

export function Historic<T extends Entity>(configuration: HistoricConfiguration<T>) {
    return function (Target: { new(): T }) {
        Reflect.defineMetadata(Historic, configuration, Target);
        return Target;
    };
}

export abstract class Entity {
    id: number;
}

type TestEntity<T, P extends string> = If<Is<Entity, Safe<T>>, P, never>;

type EntityKeys<T extends Entity> = {
    // @ts-ignore
    [P in keyof T]: If<Is<Array<any>, Safe<T[P]>>, TestEntity<UnArray<Safe<T[P]>>, P>, TestEntity<T[P], P>>
}[keyof T];

type References<T extends Entity> = {
    // @ts-ignore
    [P in EntityKeys<T>]?: If<Is<Array<any>, Safe<T[P]>>, ReferenceCallback<T, UnArray<Safe<T[P]>>>, ReferenceCallback<T, T[P]>>
};

export interface Entity {
    '---#!#---IS_ENTITY_TYPE---#!#---DO_NOT_USE---#!#---': never;
}
